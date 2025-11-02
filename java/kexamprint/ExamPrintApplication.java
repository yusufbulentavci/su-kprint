package kexamprint;

import kexamprint.db.*;
import kexamprint.model.*;
import kexamprint.service.*;
import kexamprint.util.ResourceLoader;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

/**
 * Main application for generating exam papers and signature forms
 *
 * Workflow:
 * 1. Load data from database
 * 2. Validate data and files
 * 3. If validation passes:
 *    - Assign questions to students
 *    - Generate exam papers (for written exams)
 *    - Generate signature forms (for all exams)
 * 4. If validation fails:
 *    - Generate validation report
 *    - Terminate
 */
public class ExamPrintApplication {

    private final DatabaseConfig dbConfig;
    private final WrittenExamRepository writtenExamRepo;
    private final OralExamRepository oralExamRepo;
    private final TaramaRepository taramaRepo;
    private final QuestionAssignmentRepository assignmentRepo;

    private final ValidationService validationService;
    private final QuestionAssignmentService assignmentService;
    private final ValidationReportWriter reportWriter;
    private final PrintService printService;

    private String outputDir;

    public ExamPrintApplication() {
        // Initialize database
        this.dbConfig = new DatabaseConfig();
        this.writtenExamRepo = new WrittenExamRepository(dbConfig);
        this.oralExamRepo = new OralExamRepository(dbConfig);
        this.taramaRepo = new TaramaRepository(dbConfig);
        this.assignmentRepo = new QuestionAssignmentRepository(dbConfig);

        // Initialize services
        this.validationService = new ValidationService();
        this.assignmentService = new QuestionAssignmentService();
        this.reportWriter = new ValidationReportWriter();
        this.printService = new PrintService();

        // Default output directory
        this.outputDir = ResourceLoader.getOutputBaseDir();
    }

    /**
     * Main execution method
     */
    public void run() throws Exception {
        System.out.println("=== Exam Print System ===");
        System.out.println();

        // Test database connection
        if (!testDatabaseConnection()) {
            System.err.println("ERROR: Database connection failed. Exiting.");
            return;
        }

        // Phase 1: Load data
        System.out.println("[Phase 1] Loading data from database...");
        DataSnapshot data = loadData();
        System.out.println("  Written exam announcements: " + data.writtenAnnouncements.size());
        System.out.println("  Oral exam announcements: " + data.oralAnnouncements.size());
        System.out.println("  Questions loaded: " + data.allQuestions.size());
        System.out.println();

        // Phase 2: Process by day
        System.out.println("[Phase 2] Processing exams by day...");
        System.out.println();

        // Check if assignments already exist
        int existingCount = assignmentRepo.count();
        if (existingCount > 0) {
            System.out.println("  WARNING: Found " + existingCount + " existing assignments in database");
            System.out.println("  Clearing existing assignments...");
            assignmentRepo.deleteAll();
            System.out.println("  Cleared " + existingCount + " assignments");
            System.out.println();
        }

        // Group data by day
        Map<Integer, DayData> dataByDay = groupDataByDay(data);

        int successfulDays = 0;
        int failedDays = 0;

        for (Integer day : new TreeSet<>(dataByDay.keySet())) {
            System.out.println("=== Processing Day " + day + " ===");
            DayData dayData = dataByDay.get(day);

            // Validate this day
            ValidationResult dayValidation = validateDay(dayData, day);

            if (!dayValidation.isValid()) {
                System.err.println("  Day " + day + " FAILED validation:");
                reportWriter.printSummary(dayValidation);

                // Write day-specific report
                String dayDir = outputDir + "/day-" + day;
                Path reportPath = reportWriter.writeReport(
                    dayValidation,
                    dayDir,
                    "validation_report"
                );
                System.err.println("  Validation report: " + reportPath);
                System.err.println("  Skipping Day " + day);
                System.out.println();
                failedDays++;
                continue;
            }

            System.out.println("  Day " + day + " validation PASSED");

            // Assign questions for this day
            int dayAssignments = assignQuestionsForDay(dayData);
            System.out.println("  Assigned " + dayAssignments + " questions");

            // Generate PDFs for this day
            generatePDFsForDay(dayData, day);
            System.out.println("  Day " + day + " completed successfully");
            System.out.println();
            successfulDays++;
        }

        System.out.println();

        System.out.println("=== COMPLETE ===");
        System.out.println("Successful days: " + successfulDays);
        System.out.println("Failed days: " + failedDays);
        System.out.println("Output directory: " + outputDir);
    }

    /**
     * Tests database connection
     */
    private boolean testDatabaseConnection() {
        System.out.println("Testing database connection...");
        boolean connected = dbConfig.testConnection();
        if (connected) {
            System.out.println("  Database connection OK");
        } else {
            System.err.println("  Database connection FAILED");
        }
        return connected;
    }

    /**
     * Loads all data from database
     */
    private DataSnapshot loadData() throws SQLException {
        DataSnapshot data = new DataSnapshot();

        data.writtenAnnouncements = writtenExamRepo.findAll();
        data.oralAnnouncements = oralExamRepo.findAll();
        data.allQuestions = taramaRepo.findAll();

        return data;
    }

    /**
     * Groups all data by day
     */
    private Map<Integer, DayData> groupDataByDay(DataSnapshot data) {
        Map<Integer, DayData> dataByDay = new TreeMap<>();

        // Group written announcements by day
        for (WrittenExamAnnouncement announcement : data.writtenAnnouncements) {
            Integer day = announcement.getDay();
            if (day != null) {
                dataByDay.computeIfAbsent(day, k -> new DayData(k))
                    .writtenAnnouncements.add(announcement);
            }
        }

        // Group oral announcements by day
        for (OralExamAnnouncement announcement : data.oralAnnouncements) {
            Integer day = announcement.getDay();
            if (day != null) {
                dataByDay.computeIfAbsent(day, k -> new DayData(k))
                    .oralAnnouncements.add(announcement);
            }
        }

        // All questions are available for all days
        for (DayData dayData : dataByDay.values()) {
            dayData.allQuestions = data.allQuestions;
        }

        return dataByDay;
    }

    /**
     * Validates data for a single day
     */
    private ValidationResult validateDay(DayData dayData, Integer day) {
        System.out.println("  Validating " + dayData.writtenAnnouncements.size() + " written exams...");
        System.out.println("  Validating " + dayData.oralAnnouncements.size() + " oral exams...");

        // Validate written announcements
        ValidationResult writtenValidation = validationService.validateWrittenExamAnnouncements(
            dayData.writtenAnnouncements
        );

        // Validate oral announcements
        ValidationResult oralValidation = validationService.validateOralExamAnnouncements(
            dayData.oralAnnouncements
        );

        // Group questions by exam+language for availability checking
        Map<String, List<TaramaQuestion>> questionsByExamLang =
            assignmentService.groupQuestionsByExamAndLanguage(dayData.allQuestions);

        // Create session to question mapping
        Map<String, List<TaramaQuestion>> questionsBySession = new HashMap<>();
        for (WrittenExamAnnouncement announcement : dayData.writtenAnnouncements) {
            String sessionKey = announcement.getSessionKey();
            String questionKey = assignmentService.getQuestionGroupKeyForAnnouncement(announcement);
            questionsBySession.put(sessionKey, questionsByExamLang.getOrDefault(questionKey, new ArrayList<>()));
        }

        // Validate question availability
        ValidationResult questionValidation = validationService.validateQuestionAvailability(
            dayData.writtenAnnouncements,
            questionsBySession
        );

        // Validate image files (only once, not per day)
        ValidationResult fileValidation = new ValidationResult();

        // Combine all results
        return validationService.combineResults(
            writtenValidation,
            oralValidation,
            questionValidation,
            fileValidation
        );
    }

    /**
     * Assigns questions for a single day
     */
    private int assignQuestionsForDay(DayData dayData) throws SQLException {
        // Group announcements by session
        Map<String, List<WrittenExamAnnouncement>> sessionGroups =
            assignmentService.groupBySession(dayData.writtenAnnouncements);

        // Group questions by exam+language
        Map<String, List<TaramaQuestion>> questionGroups =
            assignmentService.groupQuestionsByExamAndLanguage(dayData.allQuestions);

        // Assign questions for each session
        List<QuestionAssignment> allAssignments = new ArrayList<>();

        for (Map.Entry<String, List<WrittenExamAnnouncement>> entry : sessionGroups.entrySet()) {
            String sessionKey = entry.getKey();
            List<WrittenExamAnnouncement> sessionAnnouncements = entry.getValue();

            // Get first announcement to determine exam code and language
            WrittenExamAnnouncement firstAnnouncement = sessionAnnouncements.get(0);
            String questionKey = assignmentService.getQuestionGroupKeyForAnnouncement(firstAnnouncement);

            List<TaramaQuestion> availableQuestions = questionGroups.get(questionKey);
            if (availableQuestions == null || availableQuestions.isEmpty()) {
                continue;
            }

            // Assign questions
            List<QuestionAssignment> assignments = assignmentService.assignQuestionsForSession(
                sessionAnnouncements,
                availableQuestions
            );

            allAssignments.addAll(assignments);
        }

        // Save to database
        if (!allAssignments.isEmpty()) {
            assignmentRepo.saveAll(allAssignments);
        }

        return allAssignments.size();
    }

    /**
     * Generates PDFs for a single day
     */
    private void generatePDFsForDay(DayData dayData, Integer day) throws Exception {
        String dayDir = outputDir + "/day-" + day;
        System.out.println("  Generating PDFs to: " + dayDir);

        int writtenCount = dayData.writtenAnnouncements.size();
        int oralCount = dayData.oralAnnouncements.size();

        System.out.println("    - Written exams: " + writtenCount);
        System.out.println("    - Oral exams: " + oralCount);

        // TODO: Generate PDFs for this day
        // printService.generateExamPapers(dayData.writtenAnnouncements, dayDir + "/exam_papers");
        // printService.generateSignatureForms(dayData.writtenAnnouncements, dayData.oralAnnouncements, dayDir + "/signature_forms");
    }


    /**
     * Sets custom output directory
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Data snapshot holder
     */
    private static class DataSnapshot {
        List<WrittenExamAnnouncement> writtenAnnouncements;
        List<OralExamAnnouncement> oralAnnouncements;
        List<TaramaQuestion> allQuestions;
    }

    /**
     * Data holder for a single day
     */
    private static class DayData {
        final Integer day;
        List<WrittenExamAnnouncement> writtenAnnouncements = new ArrayList<>();
        List<OralExamAnnouncement> oralAnnouncements = new ArrayList<>();
        List<TaramaQuestion> allQuestions;

        DayData(Integer day) {
            this.day = day;
        }
    }

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        try {
            ExamPrintApplication app = new ExamPrintApplication();

            // Parse command line arguments
            if (args.length > 0) {
                app.setOutputDir(args[0]);
            }

            app.run();

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
