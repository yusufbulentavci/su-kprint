package kexamprint.model;

/**
 * Represents a question from vg12526.tarama
 * Note: id is not unique - one question can have multiple image rows
 *       realId (from real_id serial column) is unique per row
 */
public class TaramaQuestion {
    private String id;
    private Integer realId;    // real_id from tarama table (unique)
    private String imagePath;  // Raw path from database (e.g., "exam-1\images_1.jpg")
    private String examCode;   // derskodu
    private String language;   // dersdili

    public TaramaQuestion() {
    }

    public TaramaQuestion(String id, String imagePath, String examCode, String language) {
        this.id = id;
        this.imagePath = imagePath;
        this.examCode = examCode;
        this.language = language;
    }

    /**
     * Extracts the filename from the database path
     * Example: "exam-1\images_1.jpg" -> "images_1.jpg"
     */
    public String getFileName() {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        int lastBackslash = imagePath.lastIndexOf('\\');
        if (lastBackslash >= 0 && lastBackslash < imagePath.length() - 1) {
            return imagePath.substring(lastBackslash + 1);
        }
        return imagePath;  // No backslash found, return as-is
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRealId() {
        return realId;
    }

    public void setRealId(Integer realId) {
        this.realId = realId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getExamCode() {
        return examCode;
    }

    public void setExamCode(String examCode) {
        this.examCode = examCode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "TaramaQuestion{" +
                "id='" + id + '\'' +
                ", realId=" + realId +
                ", imagePath='" + imagePath + '\'' +
                ", examCode='" + examCode + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
