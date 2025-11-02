package kexamprint.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database configuration and connection management
 */
public class DatabaseConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_DATABASE = "k5";
    private static final String DEFAULT_USER = "krapp";
    private static final String DEFAULT_PASSWORD = "+SamBtg2024";

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        this(DEFAULT_HOST, DEFAULT_DATABASE, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    public DatabaseConfig(String host, String database, String user, String password) {
        this.jdbcUrl = String.format("jdbc:postgresql://%s/%s", host, database);
        this.user = user;
        this.password = password;
    }

    /**
     * Creates a new database connection
     */
    public Connection getConnection() throws SQLException {
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }

        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    /**
     * Tests the database connection
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUser() {
        return user;
    }
}
