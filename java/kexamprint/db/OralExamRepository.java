package kexamprint.db;

import kexamprint.model.OralExamAnnouncement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for accessing oral exam announcements
 */
public class OralExamRepository {

    private final DatabaseConfig dbConfig;

    public OralExamRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Fetches all oral exam announcements
     */
    public List<OralExamAnnouncement> findAll() throws SQLException {
        String sql = "SELECT " +
            "id, day, exam_code, exam_name, variant, " +
            "building, room, student_id, student_name, student_surname, " +
            "curriculum_year, curriculum_language, program_name, " +
            "education_type, has_disability " +
            "FROM kexam.oral_exam_announcements " +
            "ORDER BY day, exam_code, student_name";

        List<OralExamAnnouncement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                announcements.add(mapResultSet(rs));
            }
        }

        return announcements;
    }

    /**
     * Fetches announcements for a specific day
     */
    public List<OralExamAnnouncement> findByDay(int day) throws SQLException {
        String sql = "SELECT " +
            "id, day, exam_code, exam_name, variant, " +
            "building, room, student_id, student_name, student_surname, " +
            "curriculum_year, curriculum_language, program_name, " +
            "education_type, has_disability " +
            "FROM kexam.oral_exam_announcements " +
            "WHERE day = ? " +
            "ORDER BY exam_code, student_name";

        List<OralExamAnnouncement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, day);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    announcements.add(mapResultSet(rs));
                }
            }
        }

        return announcements;
    }

    /**
     * Maps a ResultSet row to OralExamAnnouncement
     */
    private OralExamAnnouncement mapResultSet(ResultSet rs) throws SQLException {
        OralExamAnnouncement announcement = new OralExamAnnouncement();

        announcement.setId(getInteger(rs, "id"));
        announcement.setDay(getInteger(rs, "day"));
        announcement.setExamCode(rs.getString("exam_code"));
        announcement.setExamName(rs.getString("exam_name"));
        announcement.setVariant(rs.getString("variant"));
        announcement.setBuilding(rs.getString("building"));
        announcement.setRoom(rs.getString("room"));
        announcement.setStudentId(getInteger(rs, "student_id"));
        announcement.setStudentName(rs.getString("student_name"));
        announcement.setStudentSurname(rs.getString("student_surname"));
        announcement.setCurriculumYear(rs.getString("curriculum_year"));
        announcement.setCurriculumLanguage(rs.getString("curriculum_language"));
        announcement.setProgramName(rs.getString("program_name"));
        announcement.setEducationType(rs.getString("education_type"));
        announcement.setHasDisability(rs.getBoolean("has_disability"));

        return announcement;
    }

    /**
     * Helper to get Integer from ResultSet (handles NULL)
     */
    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
