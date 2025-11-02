package kexamprint.model;

/**
 * Represents an oral exam announcement from kexam.oral_exam_announcements
 */
public class OralExamAnnouncement {
    private Integer id;
    private Integer day;
    private String examCode;
    private String examName;
    private String variant;
    private String building;
    private String room;
    private Integer studentId;
    private String studentName;
    private String studentSurname;
    private String curriculumYear;
    private String curriculumLanguage;
    private String programName;
    private String educationType;
    private Boolean hasDisability;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public String getExamCode() {
        return examCode;
    }

    public void setExamCode(String examCode) {
        this.examCode = examCode;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    // Aliases for compatibility
    public String getRoomCode() {
        return room;
    }

    public void setRoomCode(String roomCode) {
        this.room = roomCode;
    }

    public String getBuildingName() {
        return building;
    }

    public void setBuildingName(String buildingName) {
        this.building = buildingName;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentSurname() {
        return studentSurname;
    }

    public void setStudentSurname(String studentSurname) {
        this.studentSurname = studentSurname;
    }

    public String getFullStudentName() {
        if (studentName == null && studentSurname == null) {
            return null;
        }
        if (studentName == null) {
            return studentSurname;
        }
        if (studentSurname == null) {
            return studentName;
        }
        return studentName + " " + studentSurname;
    }

    public String getCurriculumYear() {
        return curriculumYear;
    }

    public void setCurriculumYear(String curriculumYear) {
        this.curriculumYear = curriculumYear;
    }

    public String getCurriculumLanguage() {
        return curriculumLanguage;
    }

    public void setCurriculumLanguage(String curriculumLanguage) {
        this.curriculumLanguage = curriculumLanguage;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getEducationType() {
        return educationType;
    }

    public void setEducationType(String educationType) {
        this.educationType = educationType;
    }

    public Boolean getHasDisability() {
        return hasDisability;
    }

    public void setHasDisability(Boolean hasDisability) {
        this.hasDisability = hasDisability;
    }
}
