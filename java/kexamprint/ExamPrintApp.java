package kexamprint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import kexamprint.model.ExamPaperData;
import kexamprint.service.PrintService;
import kexamprint.util.ResourceLoader;

/**
 * Main application for exam printing
 *
 * Usage flow:
 * 1. Fetch data from database (to be implemented)
 * 2. Build ExamPaperData objects
 * 3. Use PrintService to print papers and signature forms
 */
public class ExamPrintApp {

    public static void main(String[] args) {
        try {
            // Initialize print service
            // Uses configuration from res/kexamprint/config.properties
            // or you can override with: new PrintService("/custom/path")
            PrintService printService = new PrintService();
            String baseDir = ResourceLoader.getOutputBaseDir();

            // Example: Create sample exam papers
            List<ExamPaperData> examPapers = createSampleExamPapers();

            // Print all exam papers and signature forms for the session
            printService.printExamSession(examPapers);

            System.out.println("\n=== Printing completed successfully! ===");
            System.out.println("Output structure:");
            System.out.println("  " + baseDir + "/output/");
            System.out.println("    └── date-day/");
            System.out.println("        └── time/");
            System.out.println("            └── room/");
            System.out.println("                ├── exam-papers/");
            System.out.println("                │   └── room-day-time--seat.pdf");
            System.out.println("                └── signature-forms/");
            System.out.println("                    └── _imzo_room-date-day-time.pdf");

        } catch (Exception e) {
            System.err.println("Error during printing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example method - would be replaced with database query
     */
    private static List<ExamPaperData> createSampleExamPapers() {
        List<ExamPaperData> papers = new ArrayList<>();

        // Sample data for testing
        for (int i = 1; i <= 3; i++) {
            ExamPaperData paper = new ExamPaperData();
            paper.setExamCode("MAT101");
            paper.setExamName(ResourceLoader.getDefaultExamName());
            paper.setLanguage("uz");
            paper.setRetake(false);
            paper.setExternal(false);
            paper.setPaperVariantNumber(i);
            paper.setCourseName("Mathematics");
            paper.setRoomNumber("A-101");
            paper.setSeatNumber(i);
            paper.setExamDate(LocalDate.of(2025, 11, 15));
            paper.setDayOfWeekUz("Juma");
            paper.setTimeSlot("09:00-11:00");
            paper.setCurriculumInfo("2024-1");
            paper.setExamType("normal");

            // Image path - combine images dir from config with relative path
            String imagesDir = ResourceLoader.getExamImagesDir();
            paper.setExamImagePath(imagesDir + "/exam-image-" + i + ".pdf");

            papers.add(paper);
        }

        return papers;
    }
}
