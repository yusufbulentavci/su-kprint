package kexamprint.model;

/**
 * Student information for signature forms
 */
public class StudentSeatInfo {
    private Integer seatNumber;
    private String studentId;
    private String studentName;
    private String studentSurname;
    private String groupCode;

    public StudentSeatInfo() {
    }

    public StudentSeatInfo(Integer seatNumber, String studentId, String studentName,
                          String studentSurname, String groupCode) {
        this.seatNumber = seatNumber;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentSurname = studentSurname;
        this.groupCode = groupCode;
    }

    public String getFullName() {
        return studentId + " " + studentSurname + " " + studentName;
    }

    // Getters and Setters
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentSurname() { return studentSurname; }
    public void setStudentSurname(String studentSurname) { this.studentSurname = studentSurname; }

    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
}
