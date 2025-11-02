package kexamprint.model;

import java.time.LocalDateTime;

/**
 * Represents a question assignment from kexamprint.question_assignments table
 */
public class QuestionAssignment {
    private Integer id;
    private Integer placementId;
    private Integer studentId;
    private String roomCode;
    private String examCode;
    private String curriculumLanguage;
    private String taramaQuestionId;
    private String sessionKey;
    private LocalDateTime assignedAt;

    public QuestionAssignment() {
    }

    public QuestionAssignment(Integer placementId, Integer studentId, String roomCode,
                             String examCode, String curriculumLanguage,
                             String taramaQuestionId, String sessionKey) {
        this.placementId = placementId;
        this.studentId = studentId;
        this.roomCode = roomCode;
        this.examCode = examCode;
        this.curriculumLanguage = curriculumLanguage;
        this.taramaQuestionId = taramaQuestionId;
        this.sessionKey = sessionKey;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPlacementId() {
        return placementId;
    }

    public void setPlacementId(Integer placementId) {
        this.placementId = placementId;
    }

    /**
     * Alias for getPlacementId() - more semantic for announcement-based lookups
     */
    public Integer getAnnouncementId() {
        return placementId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getExamCode() {
        return examCode;
    }

    public void setExamCode(String examCode) {
        this.examCode = examCode;
    }

    public String getCurriculumLanguage() {
        return curriculumLanguage;
    }

    public void setCurriculumLanguage(String curriculumLanguage) {
        this.curriculumLanguage = curriculumLanguage;
    }

    public String getTaramaQuestionId() {
        return taramaQuestionId;
    }

    public void setTaramaQuestionId(String taramaQuestionId) {
        this.taramaQuestionId = taramaQuestionId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "QuestionAssignment{" +
                "id=" + id +
                ", placementId=" + placementId +
                ", studentId=" + studentId +
                ", taramaQuestionId='" + taramaQuestionId + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
