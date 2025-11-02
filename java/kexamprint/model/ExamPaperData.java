package kexamprint.model;

import java.time.LocalDate;

/**
 * Data structure for Exam Paper (Soru Kagidi) printing
 */
public class ExamPaperData {
    // Header - Line 1
    private String examCode;
    private String examName;
    private String language;
    private boolean isRetake;
    private boolean isExternal;

    // Header - Line 2
    private Integer paperVariantNumber;

    // Header - Line 3
    private String courseName;

    // Header - Line 4
    private String roomNumber;
    private Integer seatNumber;
    private LocalDate examDate;
    private String dayOfWeekUz;
    private String timeSlot;
    private String curriculumInfo;

    // Body
    private String examImagePath;
    private String examType; // "drawing", "normal", etc.

    // Metadata
    private String outputFolder;

    public ExamPaperData() {
    }

    // Computed properties
    public String getHeaderLine1() {
        return getHeaderLine1(language);
    }

    public String getHeaderLine1(String lang) {
        StringBuilder sb = new StringBuilder();
        if (isRetake) {
            sb.append(kexamprint.util.TextResources.get("exam.retake.prefix", lang));
        }
        if (isExternal) {
            sb.append(kexamprint.util.TextResources.get("exam.external.prefix", lang));
        }
        sb.append(examCode).append("-").append(language);
        sb.append("[").append(examName).append("]");
        return sb.toString();
    }

    public String getHeaderLine2() {
        return paperVariantNumber != null ? paperVariantNumber.toString() : "";
    }

    public String getHeaderLine3() {
        return courseName != null ? courseName : "";
    }

    public String getHeaderLine4() {
        StringBuilder sb = new StringBuilder();
        if (roomNumber != null) {
            sb.append(roomNumber).append("-").append(seatNumber);
        }
        sb.append("\n");
        if (examDate != null) {
            sb.append(examDate).append(" ").append(dayOfWeekUz).append(" ").append(timeSlot);
        }
        sb.append("\n");
        if (curriculumInfo != null) {
            sb.append(curriculumInfo);
        }
        return sb.toString();
    }

    public boolean isA4() {
        return !"drawing".equals(examType);
    }

    public String getFileName() {
        return String.format("%s-%s-%s--%d%s.pdf",
            roomNumber, dayOfWeekUz, timeSlot, seatNumber,
            isA4() ? "" : "-a3");
    }

    // Getters and Setters
    public String getExamCode() { return examCode; }
    public void setExamCode(String examCode) { this.examCode = examCode; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isRetake() { return isRetake; }
    public void setRetake(boolean retake) { isRetake = retake; }

    public boolean isExternal() { return isExternal; }
    public void setExternal(boolean external) { isExternal = external; }

    public Integer getPaperVariantNumber() { return paperVariantNumber; }
    public void setPaperVariantNumber(Integer paperVariantNumber) { this.paperVariantNumber = paperVariantNumber; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getDayOfWeekUz() { return dayOfWeekUz; }
    public void setDayOfWeekUz(String dayOfWeekUz) { this.dayOfWeekUz = dayOfWeekUz; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getCurriculumInfo() { return curriculumInfo; }
    public void setCurriculumInfo(String curriculumInfo) { this.curriculumInfo = curriculumInfo; }

    public String getExamImagePath() { return examImagePath; }
    public void setExamImagePath(String examImagePath) { this.examImagePath = examImagePath; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public String getOutputFolder() { return outputFolder; }
    public void setOutputFolder(String outputFolder) { this.outputFolder = outputFolder; }
}
