package com.mindos.backend.service;

import com.mindos.backend.dto.DocumentChunkResponse;
import com.mindos.backend.dto.DocumentContentResponse;
import com.mindos.backend.dto.DocumentRequest;
import com.mindos.backend.dto.DocumentResponse;
import com.mindos.backend.entity.*;
import com.mindos.backend.enums.DocumentStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.DocumentChunkRepository;
import com.mindos.backend.repository.DocumentContentRepository;
import com.mindos.backend.repository.DocumentRepository;
import com.mindos.backend.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentContentRepository documentContentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final StorageService fileStorageService;
    private final DocumentTextExtractionService textExtractionService;
    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    public DocumentService(DocumentRepository documentRepository,
                           CategoryRepository categoryRepository,
                           DocumentContentRepository documentContentRepository,
                           DocumentChunkRepository documentChunkRepository,
                           StorageService fileStorageService,
                           DocumentTextExtractionService textExtractionService,
                           DocumentChunkingService chunkingService,
                           EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.documentContentRepository = documentContentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    // 1. Get all documents for current user
    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        return documentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 2. Get one document by ID for current user
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(User user, Long id) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access this document.");
        }

        return mapToResponse(document);
    }

    // 3. Upload actual document file (Storage + Extraction + Chunking + PGVector Embeddings)
    @Transactional
    public DocumentResponse uploadDocument(User user, MultipartFile file, String title, String description, Long categoryId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload.");
        }

        // A. Store physical file via storage provider
        String storedFilePath = fileStorageService.storeFile(user.getId(), file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String derivedTitle = (title != null && !title.trim().isEmpty())
                ? title.trim()
                : originalFilename.replaceFirst("[.][^.]+$", "");

        String extension = FileStorageService.getFileExtension(originalFilename).toUpperCase();
        String fileType = extension.isEmpty() ? "OTHER" : extension;

        // Optional Category
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findByIdAndUserId(categoryId, user.getId()).orElse(null);
        }

        // B. Save initial document metadata with PROCESSING status
        Document doc = Document.builder()
                .user(user)
                .category(category)
                .fileName(originalFilename)
                .title(derivedTitle)
                .fileType(fileType)
                .fileSize(file.getSize())
                .description(description != null ? description.trim() : null)
                .fileUrl(storedFilePath)
                .status(DocumentStatus.PROCESSING)
                .build();

        Document savedDoc = documentRepository.save(doc);

        // C. Extract text content (PDF, DOCX, TXT)
        try {
            byte[] fileBytes = file.getBytes();
            String extractedText = textExtractionService.extractText(fileBytes, fileType);

            if (extractedText != null && !extractedText.trim().isEmpty()) {
                // D. Persist extracted text in document_contents
                DocumentContent content = DocumentContent.builder()
                        .document(savedDoc)
                        .extractedText(extractedText)
                        .build();
                documentContentRepository.save(content);

                // E. STEP 4.5: Generate and persist document chunks
                List<DocumentChunk> chunks = chunkingService.processAndSaveChunks(savedDoc, extractedText);

                // F. STEP 4.6: Generate and persist vector embeddings in pgvector
                if (!chunks.isEmpty()) {
                    List<DocumentChunkEmbedding> embeddings = embeddingService.generateAndSaveEmbeddings(savedDoc, chunks);
                    log.info("Document ID {} processed: {} chars, {} chunks, {} embeddings (model: {})",
                            savedDoc.getId(), extractedText.length(), chunks.size(), embeddings.size(), embeddingService.getActiveModelName());
                }

                savedDoc.setStatus(DocumentStatus.READY);
            } else {
                log.warn("Text extraction returned empty content for document ID: {}", savedDoc.getId());
                savedDoc.setStatus(DocumentStatus.FAILED);
            }
        } catch (Exception ex) {
            log.error("Document processing failed for ID {}: {}", savedDoc.getId(), ex.getMessage());
            savedDoc.setStatus(DocumentStatus.FAILED);
        }

        Document finalDoc = documentRepository.save(savedDoc);
        return mapToResponse(finalDoc);
    }

    // 4. Get Extracted Text Content, Chunk Count & Embedding Count for authenticated user
    @Transactional(readOnly = true)
    public DocumentContentResponse getDocumentContent(User user, Long documentId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to view content for this document.");
        }

        Optional<DocumentContent> contentOpt = documentContentRepository.findByDocumentId(documentId);
        long chunkCount = documentChunkRepository.countByDocumentId(documentId);
        long embeddingCount = embeddingService.getEmbeddingCountForDocument(documentId);

        if (contentOpt.isPresent()) {
            DocumentContent c = contentOpt.get();
            return DocumentContentResponse.builder()
                    .id(c.getId())
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .title(document.getTitle())
                    .fileType(document.getFileType())
                    .extractedText(c.getExtractedText())
                    .charCount(c.getCharCount())
                    .wordCount(c.getWordCount())
                    .chunkCount((int) chunkCount)
                    .embeddingCount((int) embeddingCount)
                    .hasText(true)
                    .status(document.getStatus() != null ? document.getStatus().name() : "READY")
                    .extractedAt(c.getCreatedAt())
                    .build();
        } else {
            return DocumentContentResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .title(document.getTitle())
                    .fileType(document.getFileType())
                    .extractedText("")
                    .charCount(0)
                    .wordCount(0)
                    .chunkCount((int) chunkCount)
                    .embeddingCount((int) embeddingCount)
                    .hasText(false)
                    .status(document.getStatus() != null ? document.getStatus().name() : "PROCESSING")
                    .build();
        }
    }

    // 5. Re-trigger Text Extraction, Chunking & Embedding Generation on demand
    @Transactional
    public DocumentContentResponse extractDocumentText(User user, Long documentId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to process this document.");
        }

        if (document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("No stored file found for document ID: " + documentId);
        }

        try {
            Resource fileResource = fileStorageService.loadFileAsResource(document.getFileUrl());
            String extractedText = textExtractionService.extractTextFromResource(fileResource, document.getFileType());

            DocumentContent content = documentContentRepository.findByDocumentId(documentId)
                    .orElse(DocumentContent.builder().document(document).build());

            content.setExtractedText(extractedText);
            DocumentContent savedContent = documentContentRepository.save(content);

            // Re-generate chunks
            List<DocumentChunk> chunks = chunkingService.processAndSaveChunks(document, extractedText);

            // Re-generate embeddings
            List<DocumentChunkEmbedding> embeddings = List.of();
            if (!chunks.isEmpty()) {
                embeddings = embeddingService.generateAndSaveEmbeddings(document, chunks);
            }

            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            return DocumentContentResponse.builder()
                    .id(savedContent.getId())
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .title(document.getTitle())
                    .fileType(document.getFileType())
                    .extractedText(savedContent.getExtractedText())
                    .charCount(savedContent.getCharCount())
                    .wordCount(savedContent.getWordCount())
                    .chunkCount(chunks.size())
                    .embeddingCount(embeddings.size())
                    .hasText(true)
                    .status("READY")
                    .extractedAt(savedContent.getCreatedAt())
                    .build();
        } catch (Exception ex) {
            log.error("Re-extraction and embedding failed for document ID {}: {}", documentId, ex.getMessage());
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
            throw new RuntimeException("Document processing failed: " + ex.getMessage(), ex);
        }
    }

    // 6. Get Document Chunks for authenticated user (ordered by chunkIndex)
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(User user, Long documentId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access chunks for this document.");
        }

        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .map(chunk -> DocumentChunkResponse.builder()
                        .id(chunk.getId())
                        .documentId(document.getId())
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .charCount(chunk.getCharCount())
                        .wordCount(chunk.getWordCount())
                        .createdAt(chunk.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 7. Create document metadata (JSON-only legacy/fallback)
    @Transactional
    public DocumentResponse createDocument(User user, DocumentRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = request.getFileName().replaceFirst("[.][^.]+$", "");
        }

        String fileType = request.getFileType();
        if (fileType == null || fileType.trim().isEmpty()) {
            String name = request.getFileName().toLowerCase();
            if (name.endsWith(".pdf")) fileType = "PDF";
            else if (name.endsWith(".docx") || name.endsWith(".doc")) fileType = "DOCX";
            else if (name.endsWith(".txt")) fileType = "TXT";
            else fileType = "OTHER";
        } else {
            fileType = fileType.toUpperCase();
        }

        DocumentStatus status = DocumentStatus.READY;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                status = DocumentStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                status = DocumentStatus.READY;
            }
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.getCategoryId(), user.getId()).orElse(null);
        }

        Document doc = Document.builder()
                .user(user)
                .category(category)
                .fileName(request.getFileName().trim())
                .title(title.trim())
                .fileType(fileType)
                .fileSize(request.getFileSize() != null ? request.getFileSize() : 0L)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .fileUrl(request.getStorageUrl())
                .status(status)
                .build();

        Document saved = documentRepository.save(doc);
        return mapToResponse(saved);
    }

    // 8. Update document metadata
    @Transactional
    public DocumentResponse updateDocument(User user, Long id, DocumentRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to modify this document.");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            document.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            document.setDescription(request.getDescription().trim());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                document.setStatus(DocumentStatus.valueOf(request.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), user.getId()).orElse(null);
            document.setCategory(category);
        }

        Document updated = documentRepository.save(document);
        return mapToResponse(updated);
    }

    // 9. Delete document metadata, content, chunks, embeddings, AND physical file
    @Transactional
    public void deleteDocument(User user, Long id) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to delete this document.");
        }

        // Delete vector embeddings
        embeddingService.deleteEmbeddingsByDocument(document.getId());

        // Delete document chunks
        documentChunkRepository.deleteByDocumentId(document.getId());

        // Delete extracted text content
        documentContentRepository.deleteByDocumentId(document.getId());

        // Delete physical file from storage
        if (document.getFileUrl() != null && !document.getFileUrl().trim().isEmpty()) {
            fileStorageService.deleteFile(document.getFileUrl());
        }

        documentRepository.delete(document);
    }

    // 10. Load physical document file as Resource
    @Transactional(readOnly = true)
    public Resource getDocumentFile(User user, Long id) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access this document file.");
        }

        if (document.getFileUrl() == null || document.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("No physical file associated with document ID: " + id);
        }

        return fileStorageService.loadFileAsResource(document.getFileUrl());
    }

    // Helper: Map entity to clean DTO
    public DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUser() != null ? doc.getUser().getId() : null)
                .categoryId(doc.getCategory() != null ? doc.getCategory().getId() : null)
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .fileName(doc.getFileName())
                .title(doc.getTitle() != null ? doc.getTitle() : doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .description(doc.getDescription())
                .storageUrl(doc.getFileUrl())
                .status(doc.getStatus() != null ? doc.getStatus().name() : DocumentStatus.READY.name())
                .uploadedAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
