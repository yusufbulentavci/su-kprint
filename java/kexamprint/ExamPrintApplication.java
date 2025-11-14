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
 * Two separate workflows:
 *
 * 1. ASSIGNMENT MODE (default):
 *    - Load data from database
 *    - Check for unassigned students
 *    - For each unassigned student, try to assign question if available
 *    - Skip if questions unavailable, can retry later
 *    - Incremental - can be run multiple times
 *
 * 2. PRINT MODE:
 *    - Generate PDFs for specified days only
 *    - Uses existing assignments from database
 *    - Controlled by DAYS_TO_PRINT constant
 */
public class ExamPrintApplication {

    // Configuration: Which days to generate PDFs for
    private static final int[] DAYS_TO_PRINT = {6}; // Modify this as needed
//    private static final int[] DAYS_TO_PRINT = {1, 2, 3}; // Modify this as needed


    // Operation mode
    public enum Mode {
        ASSIGN,    // Assign questions to students
        PRINT      // Generate PDFs for specified days
    }

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
    private Mode mode = Mode.PRINT; // Default mode

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
     * Main execution method - dispatches to appropriate mode
     */
    public void run() throws Exception {
        System.out.println("=== Exam Print System ===");
        System.out.println("Mode: " + mode);
        System.out.println();

        // Test database connection
        if (!testDatabaseConnection()) {
            System.err.println("ERROR: Database connection failed. Exiting.");
            return;
        }

        if (mode == Mode.ASSIGN) {
            runAssignmentMode();
        } else if (mode == Mode.PRINT) {
            runPrintMode();
        }
    }

    /**
     * ASSIGNMENT MODE: Incrementally assign questions to unassigned students
     */
    private void runAssignmentMode() throws Exception {
        System.out.println("[ASSIGNMENT MODE] Assigning questions to students...");
        System.out.println();

        // Load data
        System.out.println("[Phase 1] Loading data from database...");
        DataSnapshot data = loadData();
        System.out.println("  Written exam announcements: " + data.writtenAnnouncements.size());
        System.out.println("  Oral exam announcements: " + data.oralAnnouncements.size());
        System.out.println("  Questions loaded: " + data.allQuestions.size());
        System.out.println();

        // Check existing assignments
        int existingCount = assignmentRepo.count();
        System.out.println("  Existing assignments: " + existingCount);
        System.out.println();

        // Get existing assignments from database
        Map<Integer, QuestionAssignment> existingAssignments = new HashMap<>();
        for (QuestionAssignment assignment : assignmentRepo.findAll()) {
            existingAssignments.put(assignment.getAnnouncementId(), assignment);
        }

        // Filter to only unassigned announcements
        List<WrittenExamAnnouncement> unassigned = new ArrayList<>();
        for (WrittenExamAnnouncement announcement : data.writtenAnnouncements) {
            if (!existingAssignments.containsKey(announcement.getId())) {
                unassigned.add(announcement);
            }
        }

        System.out.println("  Unassigned announcements: " + unassigned.size());
        System.out.println();

        if (unassigned.isEmpty()) {
            System.out.println("=== COMPLETE ===");
            System.out.println("All students already have question assignments!");
            return;
        }

        // Process unassigned by day
        System.out.println("[Phase 2] Assigning questions by day...");
        Map<Integer, DayData> dataByDay = groupDataByDay(data);

        int totalAssigned = 0;
        int totalSkipped = 0;
        Map<Integer, DaySummary> daySummaries = new LinkedHashMap<>();
        Map<String, MissingQuestionInfo> missingQuestions = new LinkedHashMap<>();

        for (Integer day : new TreeSet<>(dataByDay.keySet())) {
            DayData dayData = dataByDay.get(day);

            // Filter to unassigned only
            List<WrittenExamAnnouncement> dayUnassigned = new ArrayList<>();
            for (WrittenExamAnnouncement announcement : dayData.writtenAnnouncements) {
                if (!existingAssignments.containsKey(announcement.getId())) {
                    dayUnassigned.add(announcement);
                }
            }

            if (dayUnassigned.isEmpty()) {
                continue; // Skip days with no unassigned students
            }

            System.out.println("=== Day " + day + " ===");
            System.out.println("  Unassigned students: " + dayUnassigned.size());

            // Track summary for this day
            DaySummary summary = new DaySummary(day);
            summary.totalStudents = dayData.writtenAnnouncements.size();
            summary.unassignedBefore = dayUnassigned.size();

            // Try to assign questions
            DayData unassignedDayData = new DayData(day);
            unassignedDayData.writtenAnnouncements = dayUnassigned;
            unassignedDayData.oralAnnouncements = dayData.oralAnnouncements;
            unassignedDayData.allQuestions = dayData.allQuestions;

            int assigned = assignQuestionsForDay(unassignedDayData, day, missingQuestions);
            int skipped = dayUnassigned.size() - assigned;

            summary.assigned = assigned;
            summary.skipped = skipped;
            summary.totalAssigned = dayData.writtenAnnouncements.size() - skipped;

            System.out.println("  Assigned: " + assigned);
            if (skipped > 0) {
                System.out.println("  Skipped (no questions available): " + skipped);
            }
            System.out.println();

            totalAssigned += assigned;
            totalSkipped += skipped;
            daySummaries.put(day, summary);
        }

        System.out.println("=== COMPLETE ===");
        System.out.println("Total assigned: " + totalAssigned);
        System.out.println("Total skipped: " + totalSkipped);
        System.out.println("Total in database: " + (existingCount + totalAssigned));
        System.out.println();

        // Generate reports
        writeAssignmentSummary(daySummaries, data, existingCount + totalAssigned, totalSkipped);

        if (!missingQuestions.isEmpty()) {
            writeMissingQuestionsReport(missingQuestions);
        }

        // Validate image files for all assigned questions
        System.out.println();
        System.out.println("=== Image File Validation ===");
        validateAndReportImageFiles(data.allQuestions);
    }

    /**
     * PRINT MODE: Generate PDFs for specified days
     */
    private void runPrintMode() throws Exception {
        System.out.println("[PRINT MODE] Generating PDFs for specified days...");
        System.out.print("Days to print: ");
        for (int i = 0; i < DAYS_TO_PRINT.length; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(DAYS_TO_PRINT[i]);
        }
        System.out.println();
        System.out.println();

        // Load data
        System.out.println("[Phase 1] Loading data from database...");
        DataSnapshot data = loadData();
        System.out.println("  Written exam announcements: " + data.writtenAnnouncements.size());
        System.out.println("  Oral exam announcements: " + data.oralAnnouncements.size());
        System.out.println();

        // Check assignments exist
        int assignmentCount = assignmentRepo.count();
        if (assignmentCount == 0) {
            System.err.println("ERROR: No question assignments found in database!");
            System.err.println("Please run in ASSIGN mode first to assign questions.");
            return;
        }
        System.out.println("  Found " + assignmentCount + " question assignments");
        System.out.println();

        // Group data by day
        Map<Integer, DayData> dataByDay = groupDataByDay(data);

        // Convert DAYS_TO_PRINT to Set for lookup
        Set<Integer> daysToPrint = new HashSet<>();
        for (int day : DAYS_TO_PRINT) {
            daysToPrint.add(day);
        }

        // Generate PDFs for specified days
        System.out.println("[Phase 2] Generating PDFs...");
        int successfulDays = 0;

        for (Integer day : new TreeSet<>(dataByDay.keySet())) {
            if (!daysToPrint.contains(day)) {
                continue; // Skip days not in DAYS_TO_PRINT
            }

            DayData dayData = dataByDay.get(day);
            System.out.println("=== Day " + day + " ===");

            // Validate before printing
            ValidationResult validation = validateDay(dayData, day);
//            if (!validation.isValid()) {
//                System.err.println("  Day " + day + " has validation errors:");
//                reportWriter.printSummary(validation);
//                System.err.println("  Skipping PDF generation for Day " + day);
//                System.out.println();
//                continue;
//            }

            // Generate PDFs
            generatePDFsForDay(dayData, day);
            System.out.println("  Day " + day + " PDFs generated successfully");
            System.out.println();
            successfulDays++;
        }

        System.out.println("=== COMPLETE ===");
        System.out.println("PDFs generated for " + successfulDays + " days");
        System.out.println("Output directory: " + outputDir);
    }

    /**
     * Writes assignment summary report to output directory
     */
    private void writeAssignmentSummary(Map<Integer, DaySummary> daySummaries,
                                       DataSnapshot data, int totalInDb, int totalSkipped) throws Exception {

        String reportPath = outputDir + "/assignment_summary.txt";
        System.out.println("Writing assignment summary: " + reportPath);

        java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
        java.nio.file.Files.createDirectories(outputPath);

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                java.nio.file.Paths.get(reportPath))) {

            writer.write("================================================================================");
            writer.newLine();
            writer.write("  EXAM QUESTION ASSIGNMENT SUMMARY");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("Generated: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
            writer.newLine();

            // Overall summary
            writer.write("OVERALL SUMMARY:");
            writer.newLine();
            writer.write("----------------");
            writer.newLine();
            writer.write(String.format("Total written announcements: %d", data.writtenAnnouncements.size()));
            writer.newLine();
            writer.write(String.format("Total oral announcements: %d", data.oralAnnouncements.size()));
            writer.newLine();
            writer.write(String.format("Total questions in database: %d", totalInDb));
            writer.newLine();
            writer.write(String.format("Students with missing questions: %d", totalSkipped));
            writer.newLine();
            writer.newLine();

            // Days summary
            writer.write("BY DAY:");
            writer.newLine();
            writer.write("-------");
            writer.newLine();

            for (Map.Entry<Integer, DaySummary> entry : daySummaries.entrySet()) {
                DaySummary summary = entry.getValue();
                writer.newLine();
                writer.write(String.format("Day %d:", summary.day));
                writer.newLine();
                writer.write(String.format("  Total students: %d", summary.totalStudents));
                writer.newLine();
                writer.write(String.format("  Questions assigned: %d", summary.totalAssigned));
                writer.newLine();
                writer.write(String.format("  Missing questions: %d", summary.skipped));
                writer.newLine();

                String status = summary.skipped == 0 ? "READY FOR PRINTING" : "INCOMPLETE - Missing questions";
                writer.write(String.format("  Status: %s", status));
                writer.newLine();
            }

            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("NEXT STEPS:");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();

            if (totalSkipped > 0) {
                writer.write("• Some students are still missing question assignments");
                writer.newLine();
                writer.write("• Add missing questions to the database and run assignment again");
                writer.newLine();
                writer.write("• The application will only assign questions to unassigned students");
                writer.newLine();
                writer.newLine();
            }

            // List days ready for printing
            List<Integer> readyDays = new ArrayList<>();
            for (DaySummary summary : daySummaries.values()) {
                if (summary.skipped == 0) {
                    readyDays.add(summary.day);
                }
            }

            if (!readyDays.isEmpty()) {
                writer.write("• Days ready for PDF generation: " + readyDays);
                writer.newLine();
                writer.write("• Update DAYS_TO_PRINT constant in ExamPrintApplication.java");
                writer.newLine();
                writer.write("• Run: java ExamPrintApplication print");
                writer.newLine();
            }

            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
        }

        System.out.println("Assignment summary written successfully");
    }

    /**
     * Writes detailed missing questions report to output directory
     */
    private void writeMissingQuestionsReport(Map<String, MissingQuestionInfo> missingQuestions) throws Exception {
        String reportPath = outputDir + "/missing_questions.txt";
        System.out.println("Writing missing questions report: " + reportPath);

        java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
        java.nio.file.Files.createDirectories(outputPath);

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                java.nio.file.Paths.get(reportPath))) {

            writer.write("================================================================================");
            writer.newLine();
            writer.write("  MISSING QUESTIONS REPORT");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("Generated: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
            writer.newLine();

            writer.write("This report lists all course-language combinations that are missing questions.");
            writer.newLine();
            writer.write("Students in these sessions could not be assigned questions.");
            writer.newLine();
            writer.newLine();

            writer.write("Total course-language combinations missing: " + missingQuestions.size());
            writer.newLine();
            writer.newLine();

            writer.write("================================================================================");
            writer.newLine();
            writer.newLine();

            int counter = 1;
            for (Map.Entry<String, MissingQuestionInfo> entry : missingQuestions.entrySet()) {
                MissingQuestionInfo info = entry.getValue();

                writer.write(String.format("[%d] Course: %s | Language: %s", counter++, info.examCode, info.language));
                writer.newLine();
                writer.write("--------------------------------------------------------------------------------");
                writer.newLine();

                writer.write(String.format("  Available questions: %d", info.availableQuestions));
                writer.newLine();
                writer.write(String.format("  Total students affected: %d", info.totalStudents));
                writer.newLine();
                writer.write(String.format("  Sessions affected: %d", info.sessionsAffected));
                writer.newLine();

                // Days
                writer.write("  Days affected: ");
                boolean first = true;
                for (Integer day : info.days) {
                    if (!first) writer.write(", ");
                    writer.write(String.valueOf(day));
                    first = false;
                }
                writer.newLine();
                writer.newLine();

                writer.write("  SESSION DETAILS:");
                writer.newLine();
                for (MissingQuestionInfo.SessionInfo session : info.sessions) {
                    writer.write(String.format("    • Day %d | Session: %s | Students: %d",
                        session.day, session.sessionKey, session.studentCount));
                    writer.newLine();
                }
                writer.newLine();

                // Recommendation
                int minQuestionsNeeded = 3; // At least 3 for reserve logic
                writer.write("  RECOMMENDATION:");
                writer.newLine();
                writer.write(String.format("    Add at least %d questions to database for %s (%s)",
                    minQuestionsNeeded, info.examCode, info.language));
                writer.newLine();
                writer.write("    With reserve logic: 3 questions → 2 usable, 2 questions → 2 usable, 1 question → warning");
                writer.newLine();

                writer.newLine();
                writer.write("================================================================================");
                writer.newLine();
                writer.newLine();
            }

            writer.write("NEXT STEPS:");
            writer.newLine();
            writer.write("--------------------------------------------------------------------------------");
            writer.newLine();
            writer.write("1. Add missing questions to vg12526.tarama table");
            writer.newLine();
            writer.write("2. Ensure questions have correct exam_code and language values");
            writer.newLine();
            writer.write("3. Run assignment mode again: java ExamPrintApplication assign");
            writer.newLine();
            writer.write("4. Application will automatically assign only to unassigned students");
            writer.newLine();
            writer.newLine();

            writer.write("================================================================================");
            writer.newLine();
        }

        System.out.println("Missing questions report written successfully");
    }

    /**
     * Validates image files for all questions and generates report if issues found
     */
    private void validateAndReportImageFiles(List<TaramaQuestion> allQuestions) throws Exception {
        // Get assigned question IDs from database
        Set<String> assignedQuestionIds = new HashSet<>();
        for (QuestionAssignment assignment : assignmentRepo.findAll()) {
            assignedQuestionIds.add(assignment.getTaramaQuestionId());
        }

        System.out.println("Checking image files for " + assignedQuestionIds.size() + " assigned questions...");

        // Filter questions to only those that are assigned
        List<TaramaQuestion> assignedQuestions = new ArrayList<>();
        for (TaramaQuestion question : allQuestions) {
            if (assignedQuestionIds.contains(question.getId())) {
                assignedQuestions.add(question);
            }
        }

        // Validate image files
        ValidationResult imageValidation = validationService.validateImageFiles(assignedQuestions);

        if (imageValidation.hasErrors() || imageValidation.hasWarnings()) {
            System.out.println("  Found " + imageValidation.getErrorCount() + " errors, " +
                             imageValidation.getWarningCount() + " warnings");

            // Write image validation report
            String reportPath = outputDir + "/missing_image_files.txt";
            writeMissingImageFilesReport(imageValidation, assignedQuestions.size(), reportPath);
        } else {
            System.out.println("  All image files OK!");
        }
    }

    /**
     * Writes missing image files report
     */
    private void writeMissingImageFilesReport(ValidationResult validation, int totalChecked, String reportPath)
            throws Exception {

        System.out.println("Writing missing image files report: " + reportPath);

        java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir);
        java.nio.file.Files.createDirectories(outputPath);

        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                java.nio.file.Paths.get(reportPath))) {

            writer.write("================================================================================");
            writer.newLine();
            writer.write("  MISSING IMAGE FILES REPORT");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("Generated: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
            writer.newLine();

            writer.write("This report lists all image files that are missing or unreadable for");
            writer.newLine();
            writer.write("questions that have been assigned to students.");
            writer.newLine();
            writer.newLine();

            writer.write(String.format("Total questions checked: %d", totalChecked));
            writer.newLine();
            writer.write(String.format("Errors: %d", validation.getErrorCount()));
            writer.newLine();
            writer.write(String.format("Warnings: %d", validation.getWarningCount()));
            writer.newLine();
            writer.newLine();

            if (validation.hasErrors()) {
                writer.write("================================================================================");
                writer.newLine();
                writer.write("ERRORS:");
                writer.newLine();
                writer.write("================================================================================");
                writer.newLine();
                writer.newLine();

                int errorNum = 1;
                for (ValidationResult.ValidationIssue error : validation.getErrors()) {
                    writer.write(String.format("[%d] %s", errorNum++, error.getMessage()));
                    writer.newLine();
                    if (error.getContext() != null && error.getContext().length > 0) {
                        for (Object ctx : error.getContext()) {
                            writer.write("    " + ctx.toString());
                            writer.newLine();
                        }
                    }
                    writer.newLine();
                }
            }

            if (validation.hasWarnings()) {
                writer.write("================================================================================");
                writer.newLine();
                writer.write("WARNINGS:");
                writer.newLine();
                writer.write("================================================================================");
                writer.newLine();
                writer.newLine();

                int warnNum = 1;
                for (ValidationResult.ValidationIssue warning : validation.getWarnings()) {
                    writer.write(String.format("[%d] %s", warnNum++, warning.getMessage()));
                    writer.newLine();
                    if (warning.getContext() != null && warning.getContext().length > 0) {
                        for (Object ctx : warning.getContext()) {
                            writer.write("    " + ctx.toString());
                            writer.newLine();
                        }
                    }
                    writer.newLine();
                }
            }

            writer.write("================================================================================");
            writer.newLine();
            writer.write("NOTE:");
            writer.newLine();
            writer.write("================================================================================");
            writer.newLine();
            writer.write("When printing PDFs, exam papers with missing images will still be generated");
            writer.newLine();
            writer.write("but will show placeholder text instead of the question image.");
            writer.newLine();
            writer.newLine();

            writer.write("NEXT STEPS:");
            writer.newLine();
            writer.write("1. Locate the missing image files");
            writer.newLine();
            writer.write("2. Copy them to: " + ResourceLoader.getExamImagesDir());
            writer.newLine();
            writer.write("3. Ensure filenames match exactly (case-sensitive)");
            writer.newLine();
            writer.write("4. Re-run assignment mode to validate again");
            writer.newLine();
            writer.newLine();

            writer.write("================================================================================");
            writer.newLine();
        }

        System.out.println("Missing image files report written successfully");
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
    private int assignQuestionsForDay(DayData dayData, Integer day,
                                      Map<String, MissingQuestionInfo> missingQuestions) throws SQLException {
        // Group questions by exam+language
        Map<String, List<TaramaQuestion>> questionGroups =
            assignmentService.groupQuestionsByExamAndLanguage(dayData.allQuestions);

        // Group announcements by exam+language FIRST, then by session
        Map<String, Map<String, List<WrittenExamAnnouncement>>> examSessionGroups = new LinkedHashMap<>();

        for (WrittenExamAnnouncement announcement : dayData.writtenAnnouncements) {
            String examKey = assignmentService.getQuestionGroupKeyForAnnouncement(announcement);
            String sessionKey = announcement.getSessionKey();

            examSessionGroups
                .computeIfAbsent(examKey, k -> new LinkedHashMap<>())
                .computeIfAbsent(sessionKey, k -> new ArrayList<>())
                .add(announcement);
        }

        // Sort announcements within each group by seat number
        for (Map<String, List<WrittenExamAnnouncement>> sessionGroups : examSessionGroups.values()) {
            for (List<WrittenExamAnnouncement> announcements : sessionGroups.values()) {
                announcements.sort(Comparator.comparing(WrittenExamAnnouncement::getSeatNo,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            }
        }

        // Assign questions for each exam+language+session group
        List<QuestionAssignment> allAssignments = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<WrittenExamAnnouncement>>> examEntry : examSessionGroups.entrySet()) {
            String examKey = examEntry.getKey();
            Map<String, List<WrittenExamAnnouncement>> sessionGroups = examEntry.getValue();

            // Get questions for this exam+language
            List<TaramaQuestion> availableQuestions = questionGroups.get(examKey);

            // Get unique question count
            Set<String> uniqueQuestions = new HashSet<>();
            if (availableQuestions != null) {
                for (TaramaQuestion q : availableQuestions) {
                    uniqueQuestions.add(q.getId());
                }
            }
            int questionCount = uniqueQuestions.size();

            // Parse exam code and language from key
            String[] parts = examKey.split(":");
            String examCode = parts[0];
            String language = parts[1];

            if (questionCount == 0) {
                // Track missing questions for all sessions of this exam
                String missingKey = examKey;
                MissingQuestionInfo info = missingQuestions.get(missingKey);
                if (info == null) {
                    info = new MissingQuestionInfo();
                    info.examCode = examCode;
                    info.language = language;
                    info.availableQuestions = 0;
                    missingQuestions.put(missingKey, info);
                }

                // Add all sessions for this exam
                for (Map.Entry<String, List<WrittenExamAnnouncement>> sessionEntry : sessionGroups.entrySet()) {
                    String sessionKey = sessionEntry.getKey();
                    List<WrittenExamAnnouncement> sessionAnnouncements = sessionEntry.getValue();
                    info.addSession(sessionKey, sessionAnnouncements.size(), day);
                }
                continue;
            }

            // Assign questions for each session of this exam
            for (Map.Entry<String, List<WrittenExamAnnouncement>> sessionEntry : sessionGroups.entrySet()) {
                List<WrittenExamAnnouncement> sessionAnnouncements = sessionEntry.getValue();

                // Assign questions
                List<QuestionAssignment> assignments = assignmentService.assignQuestionsForSession(
                    sessionAnnouncements,
                    availableQuestions
                );

                allAssignments.addAll(assignments);
            }
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

        // Load all assignments
        Map<Integer, QuestionAssignment> assignmentsByAnnouncementId = new HashMap<>();
        for (QuestionAssignment assignment : assignmentRepo.findAll()) {
            assignmentsByAnnouncementId.put(assignment.getAnnouncementId(), assignment);
        }

        // Load all questions for lookup - use realId as key for printing
        Map<Integer, TaramaQuestion> questionsByRealId = new HashMap<>();
        for (TaramaQuestion question : dayData.allQuestions) {
            if (question.getRealId() != null) {
                questionsByRealId.put(question.getRealId(), question);
            }
        }

        // Generate exam papers for written exams
        // Create explicit pairing using announcement ID
        List<ExamPaperData> examPapers = new ArrayList<>();
        Map<Integer, WrittenExamAnnouncement> paperIdToAnnouncement = new HashMap<>();

        int paperId = 0;
        for (WrittenExamAnnouncement announcement : dayData.writtenAnnouncements) {
            ExamPaperData paperData = createExamPaperData(
                announcement,
                assignmentsByAnnouncementId.get(announcement.getId()),
                questionsByRealId,
                dayDir
            );
            examPapers.add(paperData);
            paperIdToAnnouncement.put(paperId, announcement);
            paperId++;

            System.out.println("DEBUG: Created paper #" + (paperId-1) +
                " for student " + announcement.getStudentId() +
                " room=" + announcement.getRoom() +
                " seat=" + announcement.getSeatNo() +
                " time=" + announcement.getStartTime() + "-" + announcement.getEndTime() +
                " folder=" + paperData.getOutputFolder());
        }

        // Print exam papers
        System.out.println("    Printing exam papers...");
        printService.printExamPapers(examPapers);

        // Generate signature forms - use explicit paper-to-announcement mapping
        System.out.println("    Generating signature forms for written exams...");
        List<SignatureFormData> writtenSignatureForms = generateSignatureFormsFromPapers(
            examPapers,
            paperIdToAnnouncement,
            ResourceLoader.getWrittenExamName()
        );
        printService.printSignatureForms(writtenSignatureForms);

        // Generate signature forms for oral exams
        if (!dayData.oralAnnouncements.isEmpty()) {
            System.out.println("    Generating signature forms for oral exams...");
            List<SignatureFormData> oralSignatureForms = generateOralSignatureForms(
                dayData.oralAnnouncements,
                ResourceLoader.getOralExamName(),
                dayDir
            );
            printService.printSignatureForms(oralSignatureForms);
        }
    }

    /**
     * Creates ExamPaperData from announcement and assignment
     */
    private ExamPaperData createExamPaperData(
            WrittenExamAnnouncement announcement,
            QuestionAssignment assignment,
            Map<Integer, TaramaQuestion> questionsByRealId,
            String dayDir) {

        ExamPaperData paper = new ExamPaperData();

        // Header data from announcement
        paper.setExamCode(announcement.getExamCode());
        paper.setExamName(ResourceLoader.getWrittenExamName());
        paper.setLanguage(announcement.getCurriculumLanguage());
        paper.setCourseName(announcement.getExamName());
        paper.setBuilding(announcement.getBuilding());
        paper.setRoomNumber(announcement.getRoom());
        paper.setSeatNumber(announcement.getSeatNo());

        // Date and time (parse from announcement)
        if (announcement.getExamDate() != null) {
            try {
                paper.setExamDate(java.time.LocalDate.parse(announcement.getExamDate()));
            } catch (Exception e) {
                System.err.println("Failed to parse date: " + announcement.getExamDate());
            }
        }
        paper.setDayOfWeekUz(announcement.getDayName());
        paper.setTimeSlot(announcement.getStartTime() + "-" + announcement.getEndTime());

        // Curriculum info - translate education type to Uzbek
        String educationType = translateEducationType(announcement.getEducationType());
        String curriculumInfo = announcement.getProgramName() + " / " + educationType;
        paper.setCurriculumInfo(curriculumInfo);

        // Exam type (drawing vs normal)
        paper.setExamType(announcement.getRoomType());

        // Image path and Paper Code from assignment
        if (assignment != null) {
            // Set paper code for tracking (displayed on exam paper)
            // This is an obfuscated unique code that doesn't reveal sequential assignment
            Long paperCode = assignment.getPaperCode();
            if (paperCode != null) {
                // Format as string (already 6 digits from database formula)
                paper.setQuestionId(paperCode.toString());
            }else {
            	paper.setQuestionId(announcement.getRoom()+"-"+announcement.getStudentId()+" "+announcement.getExamDate()+" "+announcement.getStartTime() + "-" + announcement.getEndTime());
            }
            	
            	
            // Look up question using question_id (real_id) to get image
            Integer questionId = assignment.getQuestionId();
            TaramaQuestion question = questionsByRealId.get(questionId);

            if (question != null && question.getFileName() != null) {
                String imagePath = ResourceLoader.getExamImagesDir() + "/" + question.getFileName();
                paper.setExamImagePath(imagePath);
            }
        }else {
         	paper.setQuestionId(announcement.getRoom()+"-"+announcement.getSeatNo()+" "+announcement.getExamDate()+" "+announcement.getStartTime() + "-" + announcement.getEndTime());
        }
        // If no assignment or no question, examImagePath and questionId remain null

        // Output folder - organize by time and room (seats are physical locations)
        String timeSlot = announcement.getStartTime() + "-" + announcement.getEndTime();
        paper.setOutputFolder(dayDir + "/" + timeSlot + "/" + announcement.getRoom() + "/exam_papers");

        return paper;
    }

    /**
     * Translates education type to Uzbek
     */
    private String translateEducationType(String educationType) {
        if (educationType == null) {
            return "";
        }
        switch (educationType.toUpperCase()) {
            case "DAYTIME":
                return "kunduzi";
            case "EVENING":
                return "kechki";
            default:
                return educationType; // Return as-is if unknown
        }
    }

    /**
     * Generates signature forms from exam papers (grouped by room+time)
     * Uses explicit paper ID to announcement mapping
     * Ensures unique seat numbers per form (seats are physical room locations)
     */
    private List<SignatureFormData> generateSignatureFormsFromPapers(
            List<ExamPaperData> examPapers,
            Map<Integer, WrittenExamAnnouncement> paperIdToAnnouncement,
            String examName) {

        // Build paired list using explicit mapping
        // Group by output folder (room+time+date)
        Map<String, List<PaperAnnouncementPair>> sessionGroups = new HashMap<>();

        for (int i = 0; i < examPapers.size(); i++) {
            ExamPaperData paper = examPapers.get(i);
            WrittenExamAnnouncement announcement = paperIdToAnnouncement.get(i);

            if (announcement == null) {
                System.err.println("ERROR: No announcement for paper #" + i);
                continue;
            }

            // Extract parent folder (room+time+date)
            String folder = paper.getOutputFolder();
            String parentFolder = folder.substring(0, folder.lastIndexOf("/exam_papers"));

            PaperAnnouncementPair pair = new PaperAnnouncementPair();
            pair.paper = paper;
            pair.announcement = announcement;
            pair.paperId = i;  // Store paper ID for debugging

            sessionGroups.computeIfAbsent(parentFolder, k -> new ArrayList<>()).add(pair);
        }

        System.out.println("DEBUG: Generated " + sessionGroups.size() + " signature form groups from " + examPapers.size() + " papers");

        List<SignatureFormData> signatureForms = new ArrayList<>();

        for (Map.Entry<String, List<PaperAnnouncementPair>> entry : sessionGroups.entrySet()) {
            String parentFolder = entry.getKey();
            List<PaperAnnouncementPair> pairs = entry.getValue();

            if (pairs.isEmpty()) continue;

            WrittenExamAnnouncement first = pairs.get(0).announcement;
            SignatureFormData form = new SignatureFormData();
            form.setExamName(examName);
            form.setBuilding(first.getBuilding());
            form.setRoomNumber(first.getRoom());

            // Parse date
            if (first.getExamDate() != null) {
                try {
                    form.setExamDate(java.time.LocalDate.parse(first.getExamDate()));
                } catch (Exception e) {
                    System.err.println("Failed to parse date: " + first.getExamDate());
                }
            }

            form.setDayOfWeekUz(first.getDayName());
            form.setTimeSlot(first.getStartTime() + "-" + first.getEndTime());

            // Add students sorted by seat number (seats are unique in a room)
            pairs.sort(java.util.Comparator.comparing(
                p -> p.announcement.getSeatNo(),
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
            ));

            for (PaperAnnouncementPair pair : pairs) {
                WrittenExamAnnouncement announcement = pair.announcement;
                StudentSeatInfo student = new StudentSeatInfo();
                student.setSeatNumber(announcement.getSeatNo());
                student.setStudentId(announcement.getStudentId() != null ?
                    announcement.getStudentId().toString() : "");
                student.setStudentName(announcement.getStudentName());
                student.setStudentSurname(announcement.getStudentSurname());
                student.setGroupCode(announcement.getProgramName());
                form.addStudent(student);
            }

            System.out.println("DEBUG: Signature form for " + parentFolder +
                ": " + pairs.size() + " students");
            for (PaperAnnouncementPair p : pairs) {
                System.out.println("  Paper#" + p.paperId +
                    " Student=" + p.announcement.getStudentId() +
                    " Seat=" + p.announcement.getSeatNo() +
                    " Room=" + p.announcement.getRoom() +
                    " Time=" + p.announcement.getStartTime() + "-" + p.announcement.getEndTime());
            }

            // Set output folder - use same parent folder as exam papers
            form.setOutputFolder(parentFolder + "/signature_forms");

            signatureForms.add(form);
        }

        return signatureForms;
    }

    /**
     * Generates signature forms for oral exam announcements (grouped by session)
     */
    private List<SignatureFormData> generateOralSignatureForms(
            List<OralExamAnnouncement> announcements,
            String examName,
            String dayDir) {

        // Get day-to-date mapping from written exam announcements for the same day
        Map<Integer, DayDateInfo> dayToDateMap = buildDayToDateMap();

        // Group by session (room + day) - all oral exams in same room on same day
        Map<String, List<OralExamAnnouncement>> sessionGroups = new HashMap<>();
        for (OralExamAnnouncement announcement : announcements) {
            String sessionKey = announcement.getRoom() + "|" +
                               announcement.getDay();
            sessionGroups.computeIfAbsent(sessionKey, k -> new ArrayList<>()).add(announcement);
        }

        List<SignatureFormData> signatureForms = new ArrayList<>();

        for (List<OralExamAnnouncement> sessionAnnouncements : sessionGroups.values()) {
            if (sessionAnnouncements.isEmpty()) continue;

            OralExamAnnouncement first = sessionAnnouncements.get(0);
            SignatureFormData form = new SignatureFormData();
            form.setOralExam(true); // Mark as oral exam (no seat numbers)
            form.setExamName(examName);
            form.setBuilding(first.getBuilding());
            form.setRoomNumber(first.getRoom());

            // Get date info for this day from written exam announcements
            DayDateInfo dateInfo = dayToDateMap.get(first.getDay());
            if (dateInfo != null) {
                form.setExamDate(dateInfo.date);
                form.setDayOfWeekUz(dateInfo.dayName);
            } else {
                // Fallback if no written exams on this day
                form.setDayOfWeekUz("Day " + first.getDay());
            }
            form.setTimeSlot(""); // Oral exams don't have specific time slot

            // Add students sorted by name
            sessionAnnouncements.sort(java.util.Comparator.comparing(OralExamAnnouncement::getStudentSurname,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

            for (OralExamAnnouncement announcement : sessionAnnouncements) {
                StudentSeatInfo student = new StudentSeatInfo();
                student.setSeatNumber(null); // Oral exams don't have seats
                student.setStudentId(announcement.getStudentId() != null ?
                    announcement.getStudentId().toString() : "");
                student.setStudentName(announcement.getStudentName());
                student.setStudentSurname(announcement.getStudentSurname());
                student.setGroupCode(""); // Not available in oral announcements
                form.addStudent(student);
            }

            // Set output folder - oral forms go to a separate folder
            form.setOutputFolder(dayDir + "/oral/" + first.getRoom() + "/signature_forms");

            signatureForms.add(form);
        }

        return signatureForms;
    }


    /**
     * Builds a mapping from day number to date information by querying written exam announcements
     */
    private Map<Integer, DayDateInfo> buildDayToDateMap() {
        Map<Integer, DayDateInfo> dayToDateMap = new HashMap<>();

        try {
            List<WrittenExamAnnouncement> writtenAnnouncements = writtenExamRepo.findAll();
            for (WrittenExamAnnouncement announcement : writtenAnnouncements) {
                Integer day = announcement.getDay();
                if (day != null && !dayToDateMap.containsKey(day)) {
                    DayDateInfo info = new DayDateInfo();
                    // Parse exam date
                    if (announcement.getExamDate() != null) {
                        try {
                            info.date = java.time.LocalDate.parse(announcement.getExamDate());
                        } catch (Exception e) {
                            System.err.println("Failed to parse date for day " + day + ": " + announcement.getExamDate());
                        }
                    }
                    info.dayName = announcement.getDayName();
                    dayToDateMap.put(day, info);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to build day-to-date mapping: " + e.getMessage());
        }

        return dayToDateMap;
    }

    /**
     * Sets custom output directory
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * Sets operation mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Pairs exam paper with its announcement for signature form generation
     */
    private static class PaperAnnouncementPair {
        ExamPaperData paper;
        WrittenExamAnnouncement announcement;
        int paperId;  // For debugging
    }

    /**
     * Holds date information for a day
     */
    private static class DayDateInfo {
        java.time.LocalDate date;
        String dayName;
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
     * Summary statistics for a day's assignment
     */
    private static class DaySummary {
        final Integer day;
        int totalStudents;
        int unassignedBefore;
        int assigned;
        int skipped;
        int totalAssigned;

        DaySummary(Integer day) {
            this.day = day;
        }
    }

    /**
     * Tracks missing questions for a course-language combination
     */
    private static class MissingQuestionInfo {
        String examCode;
        String language;
        int availableQuestions;
        int totalStudents;
        int sessionsAffected;
        Set<Integer> days = new TreeSet<>();
        List<SessionInfo> sessions = new ArrayList<>();

        static class SessionInfo {
            String sessionKey;
            int studentCount;
            int day;

            SessionInfo(String sessionKey, int studentCount, int day) {
                this.sessionKey = sessionKey;
                this.studentCount = studentCount;
                this.day = day;
            }
        }

        void addSession(String sessionKey, int studentCount, int day) {
            this.sessions.add(new SessionInfo(sessionKey, studentCount, day));
            this.totalStudents += studentCount;
            this.sessionsAffected++;
            this.days.add(day);
        }
    }

    /**
     * Application entry point
     *
     * Usage:
     *   java ExamPrintApplication [mode] [outputDir]
     *
     * Arguments:
     *   mode       - Optional: "assign" (default) or "print"
     *   outputDir  - Optional: custom output directory (default: "output")
     *
     * Examples:
     *   java ExamPrintApplication                    # Assign mode, default output
     *   java ExamPrintApplication assign             # Assign mode, default output
     *   java ExamPrintApplication print              # Print mode, default output
     *   java ExamPrintApplication assign /tmp/out    # Assign mode, custom output
     *   java ExamPrintApplication print /tmp/out     # Print mode, custom output
     */
    public static void main(String[] args) {
        try {
            ExamPrintApplication app = new ExamPrintApplication();

            // Parse command line arguments
            if (args.length > 0) {
                String firstArg = args[0].toLowerCase();

                // Check if first argument is mode
                if (firstArg.equals("assign") || firstArg.equals("print")) {
                    Mode mode = firstArg.equals("print") ? Mode.PRINT : Mode.ASSIGN;
                    app.setMode(mode);

                    // Second argument is output directory
                    if (args.length > 1) {
                        app.setOutputDir(args[1]);
                    }
                } else {
                    // First argument is output directory (backward compatibility)
                    app.setOutputDir(args[0]);
                }
            }

            app.run();

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
