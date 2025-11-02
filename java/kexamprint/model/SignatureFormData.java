package kexamprint.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for Signature Form (Sinav Cetvel) printing
 */
public class SignatureFormData {
    private String examName;
    private String roomNumber;
    private LocalDate examDate;
    private String dayOfWeekUz;
    private String timeSlot;
    private List<StudentSeatInfo> students;
    private String outputFolder;

    public SignatureFormData() {
        this.students = new ArrayList<>();
    }

    public void addStudent(StudentSeatInfo student) {
        this.students.add(student);
    }

    public String getFileName() {
        return String.format("_imzo_%s-%s-%s-%s.pdf",
            roomNumber,
            examDate != null ? examDate.toString().replace("/", "-") : "",
            dayOfWeekUz,
            timeSlot);
    }

    public String getHeaderText() {
        return examName + " " + kexamprint.util.TextResources.getSignatureFormTitle();
    }

    public String getSubHeaderText() {
        String roomLabel = kexamprint.util.TextResources.getRoomLabel();
        return String.format("%s %s-%s %s %s",
            roomLabel, roomNumber, examDate, dayOfWeekUz, timeSlot);
    }

    // Getters and Setters
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getDayOfWeekUz() { return dayOfWeekUz; }
    public void setDayOfWeekUz(String dayOfWeekUz) { this.dayOfWeekUz = dayOfWeekUz; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public List<StudentSeatInfo> getStudents() { return students; }
    public void setStudents(List<StudentSeatInfo> students) { this.students = students; }

    public String getOutputFolder() { return outputFolder; }
    public void setOutputFolder(String outputFolder) { this.outputFolder = outputFolder; }
}
