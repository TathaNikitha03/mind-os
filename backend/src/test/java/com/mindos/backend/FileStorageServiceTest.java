package com.mindos.backend;

import com.mindos.backend.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Should store PDF file successfully and return relative path")
    void testStorePdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "DBMS_Notes.pdf",
                "application/pdf",
                "Sample PDF Content for Testing".getBytes()
        );

        String storedPath = fileStorageService.storeFile(101L, file);

        assertNotNull(storedPath);
        assertTrue(storedPath.startsWith("user_101/"));
        assertTrue(storedPath.endsWith("DBMS_Notes.pdf"));

        // Load back as Resource
        Resource resource = fileStorageService.loadFileAsResource(storedPath);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("Should store DOCX and TXT files successfully")
    void testStoreDocxAndTxtFiles() {
        MockMultipartFile docx = new MockMultipartFile(
                "file", "Specs.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX data".getBytes()
        );
        MockMultipartFile txt = new MockMultipartFile(
                "file", "Notes.txt", "text/plain", "TXT data".getBytes()
        );

        String docxPath = fileStorageService.storeFile(101L, docx);
        String txtPath = fileStorageService.storeFile(101L, txt);

        assertNotNull(docxPath);
        assertNotNull(txtPath);
        assertTrue(fileStorageService.loadFileAsResource(docxPath).exists());
        assertTrue(fileStorageService.loadFileAsResource(txtPath).exists());
    }

    @Test
    @DisplayName("Should reject invalid file extensions")
    void testRejectInvalidExtension() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "malicious.exe", "application/octet-stream", "evil code".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.storeFile(101L, badFile)
        );
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    @DisplayName("Should delete physical file from disk")
    void testDeleteFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "Temp.pdf", "application/pdf", "temp content".getBytes()
        );
        String storedPath = fileStorageService.storeFile(101L, file);
        assertTrue(fileStorageService.loadFileAsResource(storedPath).exists());

        boolean deleted = fileStorageService.deleteFile(storedPath);
        assertTrue(deleted);

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.loadFileAsResource(storedPath)
        );
    }
}
