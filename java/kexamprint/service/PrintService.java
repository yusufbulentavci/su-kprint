package kexamprint.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kexamprint.model.ExamPaperData;
import kexamprint.model.SignatureFormData;
import kexamprint.model.StudentSeatInfo;
import kexamprint.printer.ExamPaperPrinter;
import kexamprint.printer.SignatureFormPrinter;
import kexamprint.util.ResourceLoader;

/**
 * Service for managing exam printing operations
 */
public class PrintService {

    private final String baseOutputDir;

    /**
     * Create PrintService with custom output directory
     */
    public PrintService(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
    }

    /**
     * Create PrintService with default output directory from config
     */
    public PrintService() {
        this.baseOutputDir = ResourceLoader.getOutputBaseDir();
    }

    /**
     * Print a single exam paper
     */
    public void printExamPaper(ExamPaperData paperData) throws Exception {
        // Set output folder if not already set
        if (paperData.getOutputFolder() == null) {
            String folder = buildExamPaperFolder(paperData);
            paperData.setOutputFolder(folder);
        }

        ExamPaperPrinter printer = new ExamPaperPrinter(paperData);
        printer.render();
    }

    /**
     * Print multiple exam papers
     */
    public void printExamPapers(List<ExamPaperData> papers) {
        int successCount = 0;
        int failCount = 0;

        for (ExamPaperData paper : papers) {
            try {
                printExamPaper(paper);
                successCount++;
            } catch (Exception e) {
                failCount++;
                System.err.println("Failed to print exam paper for seat " +
                    paper.getSeatNumber() + " in room " + paper.getRoomNumber());
                e.printStackTrace();
            }
        }

        System.out.println(String.format("Exam Papers: %d printed, %d failed",
            successCount, failCount));
    }

    /**
     * Print a signature form
     */
    public void printSignatureForm(SignatureFormData formData) throws Exception {
        // Set output folder if not already set
        if (formData.getOutputFolder() == null) {
            String folder = buildSignatureFormFolder(formData);
            formData.setOutputFolder(folder);
        }

        SignatureFormPrinter printer = new SignatureFormPrinter(formData);
        printer.render();
    }

    /**
     * Print multiple signature forms
     */
    public void printSignatureForms(List<SignatureFormData> forms) {
        int successCount = 0;
        int failCount = 0;

        for (SignatureFormData form : forms) {
            try {
                printSignatureForm(form);
                successCount++;
            } catch (Exception e) {
                failCount++;
                System.err.println("Failed to print signature form for room " +
                    form.getRoomNumber());
                e.printStackTrace();
            }
        }

        System.out.println(String.format("Signature Forms: %d printed, %d failed",
            successCount, failCount));
    }

    /**
     * Group exam papers by session (room+time+date) and print both papers and signature forms
     */
    public void printExamSession(List<ExamPaperData> examPapers) {
        // Group papers by session
        Map<String, List<ExamPaperData>> sessionPapers = new HashMap<>();

        for (ExamPaperData paper : examPapers) {
            String sessionKey = buildSessionKey(
                paper.getRoomNumber(),
                paper.getExamDate().toString(),
                paper.getTimeSlot()
            );
            sessionPapers.computeIfAbsent(sessionKey, k -> new ArrayList<>()).add(paper);
        }

        // Print exam papers
        printExamPapers(examPapers);

        // Generate and print signature forms
        List<SignatureFormData> signatureForms = new ArrayList<>();

        for (List<ExamPaperData> sessionPaperList : sessionPapers.values()) {
            if (sessionPaperList.isEmpty()) continue;

            // Use first paper to get session info
            ExamPaperData firstPaper = sessionPaperList.get(0);

            SignatureFormData form = new SignatureFormData();
            form.setExamName(firstPaper.getExamName());
            form.setRoomNumber(firstPaper.getRoomNumber());
            form.setExamDate(firstPaper.getExamDate());
            form.setDayOfWeekUz(firstPaper.getDayOfWeekUz());
            form.setTimeSlot(firstPaper.getTimeSlot());
            form.setOutputFolder(buildSignatureFormFolder(firstPaper.getRoomNumber(),
                firstPaper.getExamDate().toString(), firstPaper.getDayOfWeekUz(),
                firstPaper.getTimeSlot()));

            // Add students from papers (we'd normally get this from database)
            for (ExamPaperData paper : sessionPaperList) {
                StudentSeatInfo student = new StudentSeatInfo();
                student.setSeatNumber(paper.getSeatNumber());
                // Note: Student details would come from database
                student.setStudentId("TBD");
                student.setStudentName("Name");
                student.setStudentSurname("Surname");
                student.setGroupCode("Group");
                form.addStudent(student);
            }

            signatureForms.add(form);
        }

        printSignatureForms(signatureForms);
    }

    /**
     * Build output folder structure:
     * output/
     *   date-day/
     *     time/
     *       room/
     *         exam-papers/
     *         signature-forms/
     */
    private String buildExamPaperFolder(ExamPaperData paper) {
        String date = paper.getExamDate().toString().replace('/', '-');
        return String.format("%s/output/%s-%s/%s/%s/exam-papers/",
            baseOutputDir,
            date,
            paper.getDayOfWeekUz(),
            paper.getTimeSlot(),
            paper.getRoomNumber()
        );
    }

    private String buildSignatureFormFolder(SignatureFormData form) {
        return buildSignatureFormFolder(
            form.getRoomNumber(),
            form.getExamDate().toString(),
            form.getDayOfWeekUz(),
            form.getTimeSlot()
        );
    }

    private String buildSignatureFormFolder(String room, String date, String day, String time) {
        String dateStr = date.replace('/', '-');
        return String.format("%s/output/%s-%s/%s/%s/signature-forms/",
            baseOutputDir,
            dateStr,
            day,
            time,
            room
        );
    }

    private String buildSessionKey(String room, String date, String time) {
        return room + "|" + date + "|" + time;
    }

    /**
     * Clean output directory
     */
    public void cleanOutputDirectory() throws Exception {
        File dir = new File(baseOutputDir);
        if (dir.exists()) {
            deleteDirectory(dir);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
