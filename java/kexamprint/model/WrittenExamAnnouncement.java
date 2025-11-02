package kexamprint.model;

import java.time.LocalTime;

/**
 * Represents a written exam announcement from kexam.written_exam_announcements
 */
public class WrittenExamAnnouncement {
    private Integer id;  // placement_id in our context
    private Integer day;
    private Integer seatNo;
    private String sessionKey;
    private String examCode;
    private String examName;
    private String variant;
    private String curriculumLanguage;
    private Integer studentId;
    private String studentName;
    private String studentSurname;
    private String examDate;  // text field
    private String dayName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String roomType;
    private String building;
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

    // Alias for compatibility with assignment service
    public Integer getPlacementId() {
        return id;
    }

    public void setPlacementId(Integer placementId) {
        this.id = placementId;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(Integer seatNo) {
        this.seatNo = seatNo;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
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

    public String getCurriculumLanguage() {
        return curriculumLanguage;
    }

    public void setCurriculumLanguage(String curriculumLanguage) {
        this.curriculumLanguage = curriculumLanguage;
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

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = examDate;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    // Alias for compatibility
    public String getDayOfWeekUz() {
        return dayName;
    }

    public void setDayOfWeekUz(String dayOfWeekUz) {
        this.dayName = dayOfWeekUz;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getTimeSlot() {
        if (startTime == null && endTime == null) {
            return null;
        }
        if (startTime == null) {
            return endTime.toString();
        }
        if (endTime == null) {
            return startTime.toString();
        }
        return startTime + "-" + endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    // Alias for compatibility
    public String getRoomCode() {
        return room;
    }

    public void setRoomCode(String roomCode) {
        this.room = roomCode;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    // Alias for compatibility
    public String getBuildingName() {
        return building;
    }

    public void setBuildingName(String buildingName) {
        this.building = buildingName;
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
