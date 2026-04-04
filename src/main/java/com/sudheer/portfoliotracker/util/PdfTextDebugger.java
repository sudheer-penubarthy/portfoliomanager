package com.sudheer.portfoliotracker.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.FileInputStream;

/**
 * Utility to extract and inspect raw PDF text for debugging parser issues
 * 
 * Usage:
 *   java -cp build/libs/portfoliotracker-0.1.0.jar com.sudheer.portfoliotracker.util.PdfTextDebugger <path-to-pdf> [password]
 */
public class PdfTextDebugger {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java PdfTextDebugger <pdf-file-path> [password]");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        String password = args.length > 1 ? args[1] : "";
        
        File file = new File(pdfPath);
        if (!file.exists()) {
            System.err.println("File not found: " + pdfPath);
            System.exit(1);
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             PDDocument document = PDDocument.load(fis, password)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            
            System.out.println("=== RAW PDF TEXT ===\n");
            System.out.println(text);
            
            System.out.println("\n\n=== LINE-BY-LINE ANALYSIS ===\n");
            String[] lines = text.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] == null ? "" : lines[i].trim();
                if (!line.isBlank()) {
                    System.out.printf("Line %3d: %s%n", i, line);
                }
            }
        }
    }
}

