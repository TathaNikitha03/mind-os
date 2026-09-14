package com.mindos.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentTextExtractionService {

    /**
     * Extract plain text from a Spring Resource based on file type.
     */
    public String extractTextFromResource(Resource resource, String fileType) {
        if (resource == null || !resource.exists()) {
            throw new IllegalArgumentException("Resource does not exist or is unreadable.");
        }
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return extractText(bytes, fileType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read document resource for extraction: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extract plain text from raw byte content based on file type.
     */
    public String extractText(byte[] fileBytes, String fileType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Cannot extract text from empty document bytes.");
        }

        String type = (fileType != null) ? fileType.trim().toUpperCase() : "TXT";
        String extracted;

        try {
            switch (type) {
                case "PDF":
                    extracted = extractFromPdf(fileBytes);
                    break;
                case "DOCX":
                case "DOC":
                    extracted = extractFromDocx(fileBytes);
                    break;
                case "TXT":
                default:
                    extracted = extractFromTxt(fileBytes);
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Text extraction failed for " + type + ": " + ex.getMessage(), ex);
        }

        String normalized = normalizeText(extracted);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("No extractable text found in document (scanned image PDFs/empty documents without OCR are not supported).");
        }

        return normalized;
    }

    // 1. PDF Extraction via Apache PDFBox 3.x
    private String extractFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    // 2. DOCX Extraction via Apache POI
    private String extractFromDocx(byte[] fileBytes) throws Exception {
        try (InputStream is = new ByteArrayInputStream(fileBytes);
             XWPFDocument xwpfDoc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(xwpfDoc)) {
            return extractor.getText();
        }
    }

    // 3. TXT Extraction (Standard UTF-8)
    private String extractFromTxt(byte[] fileBytes) {
        return new String(fileBytes, StandardCharsets.UTF_8);
    }

    // Basic text normalization: clean excessive whitespace & line breaks while preserving paragraphs
    public String normalizeText(String rawText) {
        if (rawText == null) {
            return "";
        }
        String text = rawText.replace("\r\n", "\n").replace("\r", "\n");
        // Collapse 3 or more newlines into 2
        text = text.replaceAll("\n{3,}", "\n\n");
        // Remove non-printable control characters except standard whitespace
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        return text.trim();
    }
}
