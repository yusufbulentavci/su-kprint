package kexamprint.db;

import kexamprint.model.QuestionAssignment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing question assignments
 */
public class QuestionAssignmentRepository {

    private final DatabaseConfig dbConfig;

    public QuestionAssignmentRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Saves a single question assignment
     */
    public void save(QuestionAssignment assignment) throws SQLException {
        String sql = "INSERT INTO kexamprint.question_assignments " +
            "(placement_id, student_id, room_code, exam_code, curriculum_language, " +
            "tarama_question_id, session_key) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, assignment.getPlacementId());
            stmt.setInt(2, assignment.getStudentId());
            stmt.setString(3, assignment.getRoomCode());
            stmt.setString(4, assignment.getExamCode());
            stmt.setString(5, assignment.getCurriculumLanguage());
            stmt.setString(6, assignment.getTaramaQuestionId());
            stmt.setString(7, assignment.getSessionKey());

            stmt.executeUpdate();

            // Get generated ID
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    assignment.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Saves multiple question assignments in a batch
     */
    public void saveAll(List<QuestionAssignment> assignments) throws SQLException {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO kexamprint.question_assignments " +
            "(placement_id, student_id, room_code, exam_code, curriculum_language, " +
            "tarama_question_id, session_key) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Use batch for better performance
            for (QuestionAssignment assignment : assignments) {
                stmt.setInt(1, assignment.getPlacementId());
                stmt.setInt(2, assignment.getStudentId());
                stmt.setString(3, assignment.getRoomCode());
                stmt.setString(4, assignment.getExamCode());
                stmt.setString(5, assignment.getCurriculumLanguage());
                stmt.setString(6, assignment.getTaramaQuestionId());
                stmt.setString(7, assignment.getSessionKey());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Finds assignment by placement_id
     */
    public QuestionAssignment findByPlacementId(Integer placementId) throws SQLException {
        String sql = "SELECT id, placement_id, student_id, room_code, exam_code, " +
            "curriculum_language, tarama_question_id, session_key, assigned_at " +
            "FROM kexamprint.question_assignments " +
            "WHERE placement_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, placementId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }

        return null;
    }

    /**
     * Finds all assignments for a session
     */
    public List<QuestionAssignment> findBySessionKey(String sessionKey) throws SQLException {
        String sql = "SELECT id, placement_id, student_id, room_code, exam_code, " +
            "curriculum_language, tarama_question_id, session_key, assigned_at " +
            "FROM kexamprint.question_assignments " +
            "WHERE session_key = ? " +
            "ORDER BY placement_id";

        List<QuestionAssignment> assignments = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionKey);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapResultSet(rs));
                }
            }
        }

        return assignments;
    }

    /**
     * Deletes all question assignments (for testing/reset)
     */
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM kexamprint.question_assignments";

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Counts total assignments
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM kexamprint.question_assignments";

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * Maps a ResultSet row to QuestionAssignment
     */
    private QuestionAssignment mapResultSet(ResultSet rs) throws SQLException {
        QuestionAssignment assignment = new QuestionAssignment();

        assignment.setId(rs.getInt("id"));
        assignment.setPlacementId(rs.getInt("placement_id"));
        assignment.setStudentId(rs.getInt("student_id"));
        assignment.setRoomCode(rs.getString("room_code"));
        assignment.setExamCode(rs.getString("exam_code"));
        assignment.setCurriculumLanguage(rs.getString("curriculum_language"));
        assignment.setTaramaQuestionId(rs.getString("tarama_question_id"));
        assignment.setSessionKey(rs.getString("session_key"));

        Timestamp assignedAt = rs.getTimestamp("assigned_at");
        if (assignedAt != null) {
            assignment.setAssignedAt(assignedAt.toLocalDateTime());
        }

        return assignment;
    }
}
