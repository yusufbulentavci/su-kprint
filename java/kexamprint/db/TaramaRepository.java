package kexamprint.db;

import kexamprint.model.TaramaQuestion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for accessing questions from vg12526.tarama
 */
public class TaramaRepository {

    private final DatabaseConfig dbConfig;

    public TaramaRepository(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Fetches all questions
     */
    public List<TaramaQuestion> findAll() throws SQLException {
        String sql = "SELECT id, images, derskodu, dersdili " +
            "FROM vg12526.tarama " +
            "WHERE images IS NOT NULL " +
            "ORDER BY id, images";

        List<TaramaQuestion> questions = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                questions.add(mapResultSet(rs));
            }
        }

        return questions;
    }

    /**
     * Fetches questions for a specific exam code and language
     */
    public List<TaramaQuestion> findByExamCodeAndLanguage(String examCode, String language) throws SQLException {
        String sql = "SELECT id, images, derskodu, dersdili " +
            "FROM vg12526.tarama " +
            "WHERE derskodu = ? AND dersdili = ? AND images IS NOT NULL " +
            "ORDER BY id, images";

        List<TaramaQuestion> questions = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, examCode);
            stmt.setString(2, language);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSet(rs));
                }
            }
        }

        return questions;
    }

    /**
     * Fetches questions for multiple exam code/language pairs
     * This is more efficient than calling findByExamCodeAndLanguage multiple times
     */
    public List<TaramaQuestion> findByExamCodesAndLanguages(List<String> examCodes, List<String> languages)
            throws SQLException {

        if (examCodes.isEmpty() || languages.isEmpty()) {
            return new ArrayList<>();
        }

        // Build IN clause for exam codes and languages
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ON (id) id, images, derskodu, dersdili ");
        sql.append("FROM vg12526.tarama ");
        sql.append("WHERE images IS NOT NULL ");
        sql.append("AND (");

        for (int i = 0; i < examCodes.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("(derskodu = ? AND dersdili = ?)");
        }

        sql.append(") ORDER BY id, images");

        List<TaramaQuestion> questions = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            for (int i = 0; i < examCodes.size(); i++) {
                stmt.setString(paramIndex++, examCodes.get(i));
                stmt.setString(paramIndex++, languages.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSet(rs));
                }
            }
        }

        return questions;
    }

    /**
     * Maps a ResultSet row to TaramaQuestion
     */
    private TaramaQuestion mapResultSet(ResultSet rs) throws SQLException {
        TaramaQuestion question = new TaramaQuestion();

        question.setId(rs.getString("id"));
        question.setImagePath(rs.getString("images"));
        question.setExamCode(rs.getString("derskodu"));
        question.setLanguage(rs.getString("dersdili"));

        return question;
    }
}
