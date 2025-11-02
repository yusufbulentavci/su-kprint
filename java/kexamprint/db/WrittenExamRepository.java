package kexamprint.db;

import kexamprint.model.WrittenExamAnnouncement;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for accessing written exam announcements
 */
public class WrittenExamRepository {

    private final DatabaseConfig dbConfig;

    public WrittenExamRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Fetches all written exam announcements
     */
    public List<WrittenExamAnnouncement> findAll() throws SQLException {
        String sql = "SELECT " +
            "id, day, seat_no, session_key, exam_code, exam_name, variant, " +
            "curriculum_language, student_id, student_name, student_surname, " +
            "exam_date, day_name, start_time, end_time, " +
            "room, room_type, building, program_name, education_type, has_disability " +
            "FROM kexam.written_exam_announcements " +
            "ORDER BY day, session_key, seat_no";

        List<WrittenExamAnnouncement> announcements = new ArrayList<>();

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
     * Fetches announcements for a specific session
     */
    public List<WrittenExamAnnouncement> findBySessionKey(String sessionKey) throws SQLException {
        String sql = "SELECT " +
            "id, day, seat_no, session_key, exam_code, exam_name, variant, " +
            "curriculum_language, student_id, student_name, student_surname, " +
            "exam_date, day_name, start_time, end_time, " +
            "room, room_type, building, program_name, education_type, has_disability " +
            "FROM kexam.written_exam_announcements " +
            "WHERE session_key = ? " +
            "ORDER BY seat_no";

        List<WrittenExamAnnouncement> announcements = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionKey);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    announcements.add(mapResultSet(rs));
                }
            }
        }

        return announcements;
    }

    /**
     * Maps a ResultSet row to WrittenExamAnnouncement
     */
    private WrittenExamAnnouncement mapResultSet(ResultSet rs) throws SQLException {
        WrittenExamAnnouncement announcement = new WrittenExamAnnouncement();

        announcement.setId(getInteger(rs, "id"));
        announcement.setDay(getInteger(rs, "day"));
        announcement.setSeatNo(getInteger(rs, "seat_no"));
        announcement.setSessionKey(rs.getString("session_key"));
        announcement.setExamCode(rs.getString("exam_code"));
        announcement.setExamName(rs.getString("exam_name"));
        announcement.setVariant(rs.getString("variant"));
        announcement.setCurriculumLanguage(rs.getString("curriculum_language"));
        announcement.setStudentId(getInteger(rs, "student_id"));
        announcement.setStudentName(rs.getString("student_name"));
        announcement.setStudentSurname(rs.getString("student_surname"));
        announcement.setExamDate(rs.getString("exam_date"));
        announcement.setDayName(rs.getString("day_name"));

        Time startTime = rs.getTime("start_time");
        if (startTime != null) {
            announcement.setStartTime(startTime.toLocalTime());
        }

        Time endTime = rs.getTime("end_time");
        if (endTime != null) {
            announcement.setEndTime(endTime.toLocalTime());
        }

        announcement.setRoom(rs.getString("room"));
        announcement.setRoomType(rs.getString("room_type"));
        announcement.setBuilding(rs.getString("building"));
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
