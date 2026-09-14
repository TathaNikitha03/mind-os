package com.mindos.backend.repository;

import com.mindos.backend.entity.DocumentChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentChunkEmbeddingRepository extends JpaRepository<DocumentChunkEmbedding, Long> {

    List<DocumentChunkEmbedding> findByChunkId(Long chunkId);

    Optional<DocumentChunkEmbedding> findByChunkIdAndModelName(Long chunkId, String modelName);

    void deleteByChunkId(Long chunkId);

    @Modifying
    @Query("DELETE FROM DocumentChunkEmbedding dce WHERE dce.chunk.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT COUNT(dce) FROM DocumentChunkEmbedding dce WHERE dce.chunk.document.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Native pgvector cosine similarity search filtered strictly by authenticated userId,
     * READY document status, model name, and similarity threshold.
     */
    @Query(value = """
            SELECT 
                dc.id AS chunk_id,
                d.id AS document_id,
                COALESCE(d.title, d.file_name) AS document_name,
                dc.chunk_index AS chunk_index,
                dc.content AS content,
                (1.0 - (dce.embedding <=> CAST(:queryVector AS vector))) AS similarity
            FROM document_chunk_embeddings dce
            JOIN document_chunks dc ON dc.id = dce.chunk_id
            JOIN documents d ON d.id = dc.document_id
            WHERE d.user_id = :userId
              AND d.status = 'READY'
              AND dce.model_name = :modelName
              AND (1.0 - (dce.embedding <=> CAST(:queryVector AS vector))) >= :threshold
            ORDER BY (dce.embedding <=> CAST(:queryVector AS vector)) ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> searchSimilarChunksNative(
            @Param("userId") Long userId,
            @Param("queryVector") String queryVector,
            @Param("modelName") String modelName,
            @Param("threshold") double threshold,
            @Param("topK") int topK
    );

    /**
     * JPA query for loading user's ready chunk embeddings (used for in-memory fallback/test suites)
     */
    @Query("""
            SELECT dce
            FROM DocumentChunkEmbedding dce
            JOIN FETCH dce.chunk dc
            JOIN FETCH dc.document d
            WHERE d.user.id = :userId
              AND d.status = com.mindos.backend.enums.DocumentStatus.READY
              AND dce.modelName = :modelName
            """)
    List<DocumentChunkEmbedding> findUserReadyChunkEmbeddings(
            @Param("userId") Long userId,
            @Param("modelName") String modelName
    );
}
