package com.mindos.backend;

import com.mindos.backend.service.DocumentTextExtractionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTextExtractionServiceTest {

    private DocumentTextExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new DocumentTextExtractionService();
    }

    @Test
    @DisplayName("Should extract text from PDF document using PDFBox")
    void testExtractTextFromPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(100, 700);
                stream.showText("Database Management Systems: Normalization and SQL queries.");
                stream.endText();
            }
            document.save(baos);
        }

        byte[] pdfBytes = baos.toByteArray();
        String extracted = extractionService.extractText(pdfBytes, "PDF");

        assertNotNull(extracted);
        assertTrue(extracted.contains("Database Management Systems"));
        assertTrue(extracted.contains("Normalization and SQL queries"));
    }

    @Test
    @DisplayName("Should extract text from DOCX document using Apache POI")
    void testExtractTextFromDocx() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph1 = document.createParagraph();
            XWPFRun run1 = paragraph1.createRun();
            run1.setText("Project Documentation for MindOS.");

            XWPFParagraph paragraph2 = document.createParagraph();
            XWPFRun run2 = paragraph2.createRun();
            run2.setText("The architecture includes task management and personal knowledge base.");

            document.write(baos);
        }

        byte[] docxBytes = baos.toByteArray();
        String extracted = extractionService.extractText(docxBytes, "DOCX");

        assertNotNull(extracted);
        assertTrue(extracted.contains("Project Documentation for MindOS"));
        assertTrue(extracted.contains("task management"));
    }

    @Test
    @DisplayName("Should extract text from TXT document with UTF-8 encoding")
    void testExtractTextFromTxt() {
        String originalContent = "Operating systems manage computer hardware and software resources.\nMulti-threading and process synchronization.";
        byte[] txtBytes = originalContent.getBytes(StandardCharsets.UTF_8);

        String extracted = extractionService.extractText(txtBytes, "TXT");

        assertNotNull(extracted);
        assertEquals(originalContent, extracted);
    }

    @Test
    @DisplayName("Should reject empty document / scanned PDF without extractable text")
    void testEmptyExtractionRejection() throws Exception {
        // Create blank PDF page with no text content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(baos);
        }

        byte[] blankPdfBytes = baos.toByteArray();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                extractionService.extractText(blankPdfBytes, "PDF")
        );
        assertTrue(ex.getMessage().contains("No extractable text found"));
    }
}
