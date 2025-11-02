package kexamprint.util;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for loading resources from the classpath (res directory)
 */
public class ResourceLoader {

    private static final String CONFIG_PATH = "/kexamprint/config.properties";
    private static Properties config;

    static {
        loadConfig();
    }

    /**
     * Load configuration from resources
     */
    private static void loadConfig() {
        config = new Properties();
        try (InputStream is = ResourceLoader.class.getResourceAsStream(CONFIG_PATH)) {
            if (is == null) {
                System.err.println("WARNING: Configuration file not found: " + CONFIG_PATH);
                loadDefaults();
            } else {
                config.load(is);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load configuration: " + e.getMessage());
            loadDefaults();
        }
    }

    /**
     * Load default configuration values
     */
    private static void loadDefaults() {
        config.setProperty("output.base.dir", "output");
        config.setProperty("exam.images.dir", "linked/images");
        config.setProperty("exam.written.name", "1-Oraliq nazorati");
        config.setProperty("exam.oral.name", "2-Oraliq nazorati");
        config.setProperty("exam.type.drawing", "drawing");
        config.setProperty("exam.type.written", "normal");
    }

    /**
     * Get configuration property
     */
    public static String getConfig(String key) {
        return config.getProperty(key);
    }

    /**
     * Get configuration property with default value
     */
    public static String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    /**
     * Get integer configuration property
     */
    public static int getConfigInt(String key, int defaultValue) {
        String value = config.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get output base directory
     */
    public static String getOutputBaseDir() {
        return getConfig("output.base.dir");
    }

    /**
     * Get exam images directory
     */
    public static String getExamImagesDir() {
        return getConfig("exam.images.dir");
    }

    /**
     * Get written exam name
     */
    public static String getWrittenExamName() {
        return getConfig("exam.written.name", "1-Oraliq nazorati");
    }

    /**
     * Get oral exam name
     */
    public static String getOralExamName() {
        return getConfig("exam.oral.name", "2-Oraliq nazorati");
    }

    /**
     * Get default exam name (for backward compatibility)
     */
    public static String getDefaultExamName() {
        return getWrittenExamName();
    }

    /**
     * Load a resource as InputStream from classpath
     */
    public static InputStream getResourceAsStream(String path) {
        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return ResourceLoader.class.getResourceAsStream(path);
    }

    /**
     * Check if a resource exists
     */
    public static boolean resourceExists(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return ResourceLoader.class.getResource(path) != null;
    }

    /**
     * Reload configuration (useful for testing or runtime updates)
     */
    public static void reload() {
        loadConfig();
    }
}
