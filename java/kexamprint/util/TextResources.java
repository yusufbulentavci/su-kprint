package kexamprint.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading text resources (labels, messages) from properties files
 */
public class TextResources {

    private static Properties uzbek;
    private static Properties english;
    private static String currentLanguage = "uz";

    static {
        loadResources();
    }

    private static void loadResources() {
        uzbek = loadLanguage("uz");
        english = loadLanguage("en");
    }

    private static Properties loadLanguage(String lang) {
        Properties props = new Properties();
        String path = "/kexamprint/text/labels_" + lang + ".properties";

        try (InputStream is = TextResources.class.getResourceAsStream(path)) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("WARNING: Language file not found: " + path);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load language file: " + path);
            e.printStackTrace();
        }

        return props;
    }

    /**
     * Set current language
     */
    public static void setLanguage(String lang) {
        if ("uz".equals(lang) || "en".equals(lang)) {
            currentLanguage = lang;
        }
    }

    /**
     * Get current language
     */
    public static String getLanguage() {
        return currentLanguage;
    }

    /**
     * Get text for current language
     */
    public static String get(String key) {
        return get(key, currentLanguage);
    }

    /**
     * Get text for specific language
     */
    public static String get(String key, String lang) {
        Properties props = "en".equals(lang) ? english : uzbek;
        return props.getProperty(key, key);
    }

    /**
     * Get text with default value
     */
    public static String get(String key, String defaultValue, String lang) {
        Properties props = "en".equals(lang) ? english : uzbek;
        return props.getProperty(key, defaultValue);
    }

    // Convenience methods for common labels

    public static String getExamEvaluatorLabel() {
        return get("exam.evaluator.label");
    }

    public static String getRetakePrefix() {
        return get("exam.retake.prefix");
    }

    public static String getExternalPrefix() {
        return get("exam.external.prefix");
    }

    public static String getSignatureFormTitle() {
        return get("signature.form.title");
    }

    public static String getSupervisorLabel() {
        return get("signature.supervisor.label");
    }

    public static String getRoomLabel() {
        return get("signature.room.label");
    }
}
