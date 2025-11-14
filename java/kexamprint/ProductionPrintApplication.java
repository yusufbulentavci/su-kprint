package kexamprint;

import kexamprint.db.*;
import kexamprint.model.*;
import kexamprint.service.*;
import kexamprint.util.ResourceLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Production Exam Print Application
 *
 * Generates exam papers and signature forms from production_exam_schedule table
 * - Students with assigned questions get papers with images
 * - Students without questions get papers without images
 * - All students get papers regardless of assignment status
 */
public class ProductionPrintApplication {

    private final DatabaseConfig dbConfig;
    private final TaramaRepository taramaRepo;
    private final PrintService printService;
    private String outputDir;

    public ProductionPrintApplication() {
        this.dbConfig = new DatabaseConfig();
        this.taramaRepo = new TaramaRepository(dbConfig);
        this.printService = new PrintService();
        this.outputDir = ResourceLoader.getOutputBaseDir();
    }

    public void run() throws Exception {
        System.out.println("=== Production Exam Print System ===");
        System.out.println();

        if (!testDatabaseConnection()) {
            System.err.println("ERROR: Database connection failed. Exiting.");
            return;
        }

        // Generate exam papers and signature forms
        generateExamPapers();
    }

    private boolean testDatabaseConnection() {
        System.out.println("Testing database connection...");
        boolean result = dbConfig.testConnection();
        System.out.println(result ? "✓ Database connected" : "✗ Database connection failed");
        System.out.println();
        return result;
    }

    private void generateExamPapers() throws Exception {
        System.out.println("[PRODUCTION PRINT] Generating exam papers and signature forms...");
        System.out.println();

        // Fetch all production data
        List<ProductionExamRow> examRows = fetchProductionData();
        System.out.println("Loaded " + examRows.size() + " exam records");

        // Group by room and session for signature forms
        Map<String, List<ProductionExamRow>> byRoomSession = groupByRoomSession(examRows);
        System.out.println("Found " + byRoomSession.size() + " unique room-sessions");
        System.out.println();

        // Generate exam papers
        System.out.println("Generating exam papers...");
        List<ExamPaperData> examPapers = new ArrayList<>();
        for (ProductionExamRow row : examRows) {
            ExamPaperData paper = createExamPaper(row);
            examPapers.add(paper);
        }
        printService.printExamPapers(examPapers);
        System.out.println();

        // Generate signature forms
        System.out.println("Generating signature forms...");
        List<SignatureFormData> signatureForms = new ArrayList<>();
        for (Map.Entry<String, List<ProductionExamRow>> entry : byRoomSession.entrySet()) {
            SignatureFormData form = createSignatureForm(entry.getValue());
            signatureForms.add(form);
        }
        printService.printSignatureForms(signatureForms);
        System.out.println();

        System.out.println("=== PRODUCTION PRINT COMPLETE ===");
    }

    private List<ProductionExamRow> fetchProductionData() throws SQLException {
        String sql =
            "SELECT " +
            "  p.id, p.exam_code, p.exam_name, p.student_number, p.student_name, " +
            "  p.room_name, p.room_type, p.seat_id, p.paper_code, " +
            "  p.language, p.day, p.date, p.lesson_start, p.lesson_end, " +
            "  p.start_time, p.end_time, " +
            "  qa.question_id, qa.tarama_question_id, " +
            "  t.images as question_image_path " +
            "FROM kexamprint.production_exam_schedule p " +
            "LEFT JOIN kexamprint.question_assignments qa ON p.id = qa.placement_id " +
            "LEFT JOIN vg12526.tarama t ON qa.question_id = t.real_id " +
            "ORDER BY p.room_name, p.start_time, p.seat_id";

        List<ProductionExamRow> rows = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ProductionExamRow row = new ProductionExamRow();

                row.id = rs.getInt("id");
                row.examCode = rs.getString("exam_code");
                row.examName = rs.getString("exam_name");
                row.studentNumber = rs.getString("student_number");
                row.studentName = rs.getString("student_name");
                row.roomName = rs.getString("room_name");
                row.roomType = rs.getString("room_type");
                row.seatId = rs.getInt("seat_id");

                long paperCodeVal = rs.getLong("paper_code");
                if (!rs.wasNull()) {
                    row.paperCode = paperCodeVal;
                }

                row.language = rs.getString("language");
                row.day = rs.getInt("day");
                row.date = rs.getDate("date").toLocalDate();
                row.lessonStart = rs.getInt("lesson_start");
                row.lessonEnd = rs.getInt("lesson_end");
                row.startTime = rs.getTime("start_time").toLocalTime();
                row.endTime = rs.getTime("end_time").toLocalTime();

                int questionId = rs.getInt("question_id");
                if (!rs.wasNull()) {
                    row.questionId = questionId;
                }

                row.taramaQuestionId = rs.getString("tarama_question_id");
                row.questionImagePath = rs.getString("question_image_path");

                rows.add(row);
            }
        }

        return rows;
    }

    private ExamPaperData createExamPaper(ProductionExamRow row) {
        ExamPaperData paper = new ExamPaperData();

        // Header Line 1: exam code, name, language
        paper.setExamCode(row.examCode);
        paper.setExamName(row.examName);
        paper.setLanguage(row.language);
        paper.setRetake(false);
        paper.setExternal(false);

        // Header Line 2: paper code (question ID)
        if (row.paperCode != null) {
            paper.setQuestionId(row.paperCode.toString());
        } else {
            // Fallback if no paper code
            paper.setQuestionId(row.roomName + "-" + row.seatId);
        }

        // Header Line 3: course name (same as exam name)
        paper.setCourseName(row.examName);

        // Header Line 4: room, seat, date, time
        paper.setRoomNumber(row.roomName);
        paper.setSeatNumber(row.seatId);
        paper.setExamDate(row.date);
        paper.setDayOfWeekUz(getDayOfWeekUz(row.date));
        paper.setTimeSlot(row.startTime.toString() + "-" + row.endTime.toString());
        paper.setCurriculumInfo(row.language);

        // Exam type (determines paper size)
        paper.setExamType(row.roomType);

        // Question image path (can be null for students without assignments)
        if (row.questionImagePath != null) {
            // Strip "exam-1\" prefix and normalize path separators
            String normalizedPath = row.questionImagePath;
            if (normalizedPath.startsWith("exam-1\\") || normalizedPath.startsWith("exam-1/")) {
                normalizedPath = normalizedPath.substring(7); // Remove "exam-1\" or "exam-1/"
            }
            normalizedPath = normalizedPath.replace("\\", "/"); // Normalize to forward slashes

            String imagePath = ResourceLoader.getExamImagesDir() + "/" + normalizedPath;
            paper.setExamImagePath(imagePath);
        }

        // Output folder: day/time/room structure
        String timeSlot = row.startTime.toString() + "-" + row.endTime.toString();
        String outputFolder = outputDir + "/day-" + row.day + "/" + timeSlot + "/" + row.roomName;
        paper.setOutputFolder(outputFolder);

        return paper;
    }

    private Map<String, List<ProductionExamRow>> groupByRoomSession(List<ProductionExamRow> rows) {
        Map<String, List<ProductionExamRow>> grouped = new LinkedHashMap<>();

        for (ProductionExamRow row : rows) {
            String key = row.roomName + "|" + row.date + "|" + row.startTime;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        return grouped;
    }

    private SignatureFormData createSignatureForm(List<ProductionExamRow> students) {
        if (students.isEmpty()) {
            return null;
        }

        // Use first student for room/session info
        ProductionExamRow first = students.get(0);

        SignatureFormData form = new SignatureFormData();

        // Parse room name (e.g., "boshbino-300" -> building: "boshbino", room: "300")
        String[] roomParts = first.roomName.split("-", 2);
        if (roomParts.length == 2) {
            form.setBuilding(roomParts[0]);
            form.setRoomNumber(roomParts[1]);
        } else {
            form.setRoomNumber(first.roomName);
        }

        form.setExamName(first.examCode + " - " + first.examName);
        form.setExamDate(first.date);
        form.setDayOfWeekUz(getDayOfWeekUz(first.date));
        form.setTimeSlot(first.startTime.toString() + "-" + first.endTime.toString());

        // Convert to StudentSeatInfo list
        List<StudentSeatInfo> seatInfos = new ArrayList<>();
        for (ProductionExamRow row : students) {
            StudentSeatInfo info = new StudentSeatInfo();
            info.setSeatNumber(row.seatId);
            info.setStudentId(row.studentNumber);

            // Split student name into name and surname if possible
            String[] nameParts = row.studentName.split("\\s+", 2);
            if (nameParts.length == 2) {
                info.setStudentSurname(nameParts[0]);
                info.setStudentName(nameParts[1]);
            } else {
                info.setStudentName(row.studentName);
            }

            info.setGroupCode(row.examCode + "-" + row.language);
            seatInfos.add(info);
        }
        form.setStudents(seatInfos);

        // Output folder: day/time/room structure
        String timeSlot = first.startTime.toString() + "-" + first.endTime.toString();
        String outputFolder = outputDir + "/day-" + first.day + "/" + timeSlot + "/" + first.roomName;
        form.setOutputFolder(outputFolder);

        return form;
    }

    private String getDayOfWeekUz(LocalDate date) {
        String[] daysUz = {"Yakshanba", "Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba"};
        return daysUz[date.getDayOfWeek().getValue() % 7];
    }

    // Inner class to hold production exam data
    private static class ProductionExamRow {
        int id;
        String examCode;
        String examName;
        String studentNumber;
        String studentName;
        String roomName;
        String roomType;
        int seatId;
        Long paperCode;
        String language;
        int day;
        LocalDate date;
        int lessonStart;
        int lessonEnd;
        java.time.LocalTime startTime;
        java.time.LocalTime endTime;
        Integer questionId;
        String taramaQuestionId;
        String questionImagePath;
    }

    public static void main(String[] args) {
        try {
            ProductionPrintApplication app = new ProductionPrintApplication();
            app.run();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
