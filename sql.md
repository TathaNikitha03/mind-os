-- ============================================================
-- MIND FLOW AI
-- Intelligent Personal Information, Action & Knowledge System
-- PostgreSQL Database Schema
-- ============================================================

-- ============================================================
-- 1. EXTENSIONS
-- ============================================================

-- Required later for semantic search / RAG.
-- This requires the pgvector extension to be installed.
-- If pgvector is not installed yet, comment this line for now.

-- CREATE EXTENSION IF NOT EXISTS vector;


-- ============================================================
-- 2. USERS
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(255) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 3. CATEGORIES
-- ============================================================

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    name VARCHAR(100) NOT NULL,

    description TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_user_category
        UNIQUE (user_id, name)
);


-- ============================================================
-- 4. TASKS
-- ============================================================

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    category_id BIGINT,

    title VARCHAR(200) NOT NULL,

    description TEXT,

    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

    status VARCHAR(20) NOT NULL DEFAULT 'TODO',

    due_date TIMESTAMP,

    estimated_minutes INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    completed_at TIMESTAMP,

    CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tasks_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_task_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT chk_task_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED')),

    CONSTRAINT chk_estimated_minutes
        CHECK (
            estimated_minutes IS NULL
            OR estimated_minutes > 0
        )
);


-- ============================================================
-- 5. TASK DEPENDENCIES
-- ============================================================

CREATE TABLE IF NOT EXISTS task_dependencies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    task_id BIGINT NOT NULL,

    depends_on_task_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dependency_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_dependency_parent_task
        FOREIGN KEY (depends_on_task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_no_self_dependency
        CHECK (task_id <> depends_on_task_id),

    CONSTRAINT unique_task_dependency
        UNIQUE (task_id, depends_on_task_id)
);


-- ============================================================
-- 6. TAGS
-- ============================================================

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    name VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tags_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_user_tag
        UNIQUE (user_id, name)
);


-- ============================================================
-- 7. TASK TAGS
-- Many-to-many relationship between tasks and tags
-- ============================================================

CREATE TABLE IF NOT EXISTS task_tags (
    task_id BIGINT NOT NULL,

    tag_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (task_id, tag_id),

    CONSTRAINT fk_task_tags_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 8. REMINDERS
-- ============================================================

CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    task_id BIGINT NOT NULL,

    reminder_time TIMESTAMP NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reminder_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reminder_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_reminder_status
        CHECK (
            status IN ('PENDING', 'SENT', 'CANCELLED')
        )
);


-- ============================================================
-- 9. NOTIFICATIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    title VARCHAR(200) NOT NULL,

    message TEXT NOT NULL,

    type VARCHAR(30) NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 10. DOCUMENTS
-- ============================================================

CREATE TABLE IF NOT EXISTS documents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    category_id BIGINT,

    file_name VARCHAR(255) NOT NULL,

    file_type VARCHAR(100),

    file_url TEXT NOT NULL,

    file_size BIGINT,

    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_documents_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_document_status
        CHECK (
            status IN (
                'UPLOADED',
                'PROCESSING',
                'PROCESSED',
                'FAILED'
            )
        )
);


-- ============================================================
-- 10.1. DOCUMENT CONTENTS
-- Plain text extracted from documents (PDF, DOCX, TXT)
-- ============================================================

CREATE TABLE IF NOT EXISTS document_contents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    document_id BIGINT NOT NULL,

    extracted_text TEXT NOT NULL,

    char_count INTEGER,

    word_count INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_contents_doc
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_document_contents_doc_id
        UNIQUE (document_id)
);


-- ============================================================
-- 11. DOCUMENT ENTITIES
-- Information extracted from documents by AI
-- ============================================================

CREATE TABLE IF NOT EXISTS document_entities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    document_id BIGINT NOT NULL,

    entity_type VARCHAR(50) NOT NULL,

    entity_value TEXT NOT NULL,

    confidence NUMERIC(5,4),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_entity_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_entity_confidence
        CHECK (
            confidence IS NULL
            OR (
                confidence >= 0
                AND confidence <= 1
            )
        )
);


-- ============================================================
-- 12. DOCUMENT CHUNKS
-- Sequential overlapping text chunks for retrieval and RAG
-- ============================================================

CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    document_id BIGINT NOT NULL,

    chunk_index INTEGER NOT NULL,

    content TEXT NOT NULL,

    char_count INTEGER,

    word_count INTEGER,

    -- Note: Vector embeddings will be added in Step 5
    -- embedding VECTOR(384),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_document_chunk
        UNIQUE (document_id, chunk_index)
);


-- ============================================================
-- 12.1 DOCUMENT CHUNK EMBEDDINGS (STEP 4.6)
-- Vector embeddings stored in PostgreSQL pgvector (1536 dim)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk_embeddings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    chunk_id BIGINT NOT NULL,

    embedding VECTOR(1536) NOT NULL,

    model_name VARCHAR(100) NOT NULL,

    dimension INTEGER NOT NULL DEFAULT 1536,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chunk_embedding_chunk
        FOREIGN KEY (chunk_id)
        REFERENCES document_chunks(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_chunk_model_embedding
        UNIQUE (chunk_id, model_name)
);


-- ============================================================
-- 13. AI EXTRACTIONS
-- Stores structured output produced by AI
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_extractions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    document_id BIGINT,

    user_id BIGINT NOT NULL,

    extraction_type VARCHAR(50) NOT NULL,

    result JSONB NOT NULL,

    model_name VARCHAR(100),

    confidence NUMERIC(5,4),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_extraction_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_extraction_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_ai_confidence
        CHECK (
            confidence IS NULL
            OR (
                confidence >= 0
                AND confidence <= 1
            )
        )
);


-- ============================================================
-- 14. AI INTERACTIONS
-- Stores conversations/questions asked to the AI
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_interactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    session_id VARCHAR(100),

    query TEXT NOT NULL,

    response TEXT NOT NULL,

    model_name VARCHAR(100),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_interaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 15. ACTIVITY LOGS
-- Tracks important actions performed by users/system
-- ============================================================

CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    action VARCHAR(100) NOT NULL,

    entity_type VARCHAR(50),

    entity_id BIGINT,

    metadata JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- ============================================================
-- 16. INDEXES
-- Improve frequently used queries
-- ============================================================

-- USERS
CREATE INDEX IF NOT EXISTS idx_users_email
ON users(email);


-- TASKS
CREATE INDEX IF NOT EXISTS idx_tasks_user
ON tasks(user_id);

CREATE INDEX IF NOT EXISTS idx_tasks_status
ON tasks(status);

CREATE INDEX IF NOT EXISTS idx_tasks_priority
ON tasks(priority);

CREATE INDEX IF NOT EXISTS idx_tasks_due_date
ON tasks(due_date);

CREATE INDEX IF NOT EXISTS idx_tasks_user_status
ON tasks(user_id, status);

CREATE INDEX IF NOT EXISTS idx_tasks_user_due_date
ON tasks(user_id, due_date);


-- TASK DEPENDENCIES
CREATE INDEX IF NOT EXISTS idx_dependency_task
ON task_dependencies(task_id);

CREATE INDEX IF NOT EXISTS idx_dependency_parent
ON task_dependencies(depends_on_task_id);


-- REMINDERS
CREATE INDEX IF NOT EXISTS idx_reminders_user
ON reminders(user_id);

CREATE INDEX IF NOT EXISTS idx_reminders_task
ON reminders(task_id);

CREATE INDEX IF NOT EXISTS idx_reminders_time
ON reminders(reminder_time);


-- NOTIFICATIONS
CREATE INDEX IF NOT EXISTS idx_notifications_user
ON notifications(user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
ON notifications(user_id, is_read);


-- DOCUMENTS
CREATE INDEX IF NOT EXISTS idx_documents_user
ON documents(user_id);

CREATE INDEX IF NOT EXISTS idx_documents_status
ON documents(status);


-- DOCUMENT ENTITIES
CREATE INDEX IF NOT EXISTS idx_document_entities_document
ON document_entities(document_id);

CREATE INDEX IF NOT EXISTS idx_document_entities_type
ON document_entities(entity_type);


-- DOCUMENT CHUNKS
CREATE INDEX IF NOT EXISTS idx_document_chunks_document
ON document_chunks(document_id);


-- AI EXTRACTIONS
CREATE INDEX IF NOT EXISTS idx_ai_extractions_user
ON ai_extractions(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_extractions_document
ON ai_extractions(document_id);

CREATE INDEX IF NOT EXISTS idx_ai_extractions_type
ON ai_extractions(extraction_type);


-- AI INTERACTIONS
CREATE INDEX IF NOT EXISTS idx_ai_interactions_user
ON ai_interactions(user_id);

CREATE INDEX IF NOT EXISTS idx_ai_interactions_session
ON ai_interactions(session_id);


-- ACTIVITY LOGS
CREATE INDEX IF NOT EXISTS idx_activity_user
ON activity_logs(user_id);

CREATE INDEX IF NOT EXISTS idx_activity_created
ON activity_logs(created_at);


-- ============================================================
-- 17. UPDATED_AT TRIGGER FUNCTION
-- Automatically updates updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- USERS
DROP TRIGGER IF EXISTS update_users_updated_at
ON users;

CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


-- TASKS
DROP TRIGGER IF EXISTS update_tasks_updated_at
ON tasks;

CREATE TRIGGER update_tasks_updated_at
BEFORE UPDATE ON tasks
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


-- DOCUMENTS
DROP TRIGGER IF EXISTS update_documents_updated_at
ON documents;

CREATE TRIGGER update_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- 18. DATABASE COMPLETE
-- ============================================================

SELECT 'MindFlow AI database schema created successfully!'
AS message;