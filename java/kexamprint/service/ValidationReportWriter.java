package kexamprint.service;

import kexamprint.model.ValidationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Writes validation reports to text files
 */
public class ValidationReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Writes validation result to a report file
     *
     * @param result The validation result to write
     * @param outputDir The output directory for the report
     * @param reportName The base name for the report file (without extension)
     * @return The path to the generated report file
     */
    public Path writeReport(ValidationResult result, String outputDir, String reportName) throws IOException {
        // Create output directory if it doesn't exist
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        // Create report file
        String fileName = reportName + ".txt";
        Path reportPath = outputPath.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writeReportHeader(writer, result);
            writer.newLine();

            writeSummary(writer, result);
            writer.newLine();

            if (result.hasErrors()) {
                writeErrors(writer, result);
                writer.newLine();
            }

            if (result.hasWarnings()) {
                writeWarnings(writer, result);
            }

            writeReportFooter(writer);
        }

        return reportPath;
    }

    private void writeReportHeader(BufferedWriter writer, ValidationResult result) throws IOException {
        writer.write("================================================================================");
        writer.newLine();
        writer.write("  EXAM PRINT VALIDATION REPORT");
        writer.newLine();
        writer.write("================================================================================");
        writer.newLine();
        writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        writer.newLine();
    }

    private void writeSummary(BufferedWriter writer, ValidationResult result) throws IOException {
        writer.write("SUMMARY:");
        writer.newLine();
        writer.write("--------");
        writer.newLine();
        writer.write(String.format("Status: %s", result.isValid() ? "PASSED" : "FAILED"));
        writer.newLine();
        writer.write(String.format("Errors: %d", result.getErrorCount()));
        writer.newLine();
        writer.write(String.format("Warnings: %d", result.getWarningCount()));
        writer.newLine();
    }

    private void writeErrors(BufferedWriter writer, ValidationResult result) throws IOException {
        writer.write("ERRORS:");
        writer.newLine();
        writer.write("-------");
        writer.newLine();

        // Group errors by category for better readability
        Map<String, List<ValidationResult.ValidationIssue>> errorsByCategory = new LinkedHashMap<>();
        for (ValidationResult.ValidationIssue error : result.getErrors()) {
            String category = error.getCategory();
            errorsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(error);
        }

        int errorNum = 1;
        for (Map.Entry<String, List<ValidationResult.ValidationIssue>> entry : errorsByCategory.entrySet()) {
            String category = entry.getKey();
            List<ValidationResult.ValidationIssue> errors = entry.getValue();

            // Write category header
            writer.newLine();
            writer.write("=== " + category + " (" + errors.size() + " errors) ===");
            writer.newLine();

            // Special formatting for MISSING_QUESTIONS_DETAIL
            if ("MISSING_QUESTIONS_DETAIL".equals(category)) {
                writer.write("Missing questions by Course-Language:");
                writer.newLine();
                writer.write("-------------------------------------");
                writer.newLine();
                for (ValidationResult.ValidationIssue error : errors) {
                    writer.write(String.format("  • %s", error.getMessage()));
                    if (error.getContext() != null && error.getContext().length > 0) {
                        writer.write(" - ");
                        for (int i = 0; i < error.getContext().length; i++) {
                            if (i > 0) writer.write(", ");
                            writer.write(error.getContext()[i].toString());
                        }
                    }
                    writer.newLine();
                }
            } else {
                // Standard error formatting
                for (ValidationResult.ValidationIssue error : errors) {
                    writer.write(String.format("%d. %s", errorNum++, error.toString()));
                    writer.newLine();
                }
            }
        }
    }

    private void writeWarnings(BufferedWriter writer, ValidationResult result) throws IOException {
        writer.write("WARNINGS:");
        writer.newLine();
        writer.write("---------");
        writer.newLine();

        int warningNum = 1;
        for (ValidationResult.ValidationIssue warning : result.getWarnings()) {
            writer.write(String.format("%d. %s", warningNum++, warning.toString()));
            writer.newLine();
        }
    }

    private void writeReportFooter(BufferedWriter writer) throws IOException {
        writer.newLine();
        writer.write("================================================================================");
        writer.newLine();
        writer.write("End of Report");
        writer.newLine();
        writer.write("================================================================================");
    }

    /**
     * Writes a simple summary report to console
     */
    public void printSummary(ValidationResult result) {
        System.out.println("=== Validation Summary ===");
        System.out.println("Status: " + (result.isValid() ? "PASSED" : "FAILED"));
        System.out.println("Total Errors: " + result.getErrorCount());
        System.out.println("Total Warnings: " + result.getWarningCount());

        if (result.hasErrors()) {
            // Group errors by category
            Map<String, Integer> errorsByCategory = new LinkedHashMap<>();
            for (ValidationResult.ValidationIssue error : result.getErrors()) {
                String category = error.getCategory();
                errorsByCategory.put(category, errorsByCategory.getOrDefault(category, 0) + 1);
            }

            System.out.println("\nErrors by category:");
            for (Map.Entry<String, Integer> entry : errorsByCategory.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }

            System.out.println("\nFirst 5 errors:");
            int count = 0;
            for (ValidationResult.ValidationIssue error : result.getErrors()) {
                if (count++ >= 5) break;
                System.out.println("  - [" + error.getCategory() + "] " + error.getMessage());
            }
            if (result.getErrorCount() > 5) {
                System.out.println("  ... and " + (result.getErrorCount() - 5) + " more errors");
            }
        }

        if (result.hasWarnings()) {
            System.out.println("\nWarnings:");
            int count = 0;
            for (ValidationResult.ValidationIssue warning : result.getWarnings()) {
                if (count++ >= 3) break;
                System.out.println("  - " + warning.getMessage());
            }
            if (result.getWarningCount() > 3) {
                System.out.println("  ... and " + (result.getWarningCount() - 3) + " more warnings");
            }
        }
    }
}
