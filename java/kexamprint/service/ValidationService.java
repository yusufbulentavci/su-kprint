package kexamprint.service;

import kexamprint.model.*;
import kexamprint.util.ResourceLoader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Service for validating exam data and files before PDF generation
 */
public class ValidationService {

    private static final Set<String> VALID_ROOM_TYPES = Set.of("aud", "drawing", "pclab");
    private static final int MIN_QUESTIONS_FOR_RESERVE = 3;

    private final String examImagesDir;

    public ValidationService() {
        this.examImagesDir = ResourceLoader.getExamImagesDir();
    }

    /**
     * Validates written exam announcements
     * Validates ALL announcements, collecting all errors
     */
    public ValidationResult validateWrittenExamAnnouncements(List<WrittenExamAnnouncement> announcements) {
        ValidationResult result = new ValidationResult();

        if (announcements == null || announcements.isEmpty()) {
            result.addError("DATA", "No written exam announcements found");
            return result;
        }

        // Validate ALL announcements - don't stop at first error
        for (WrittenExamAnnouncement announcement : announcements) {
            validateWrittenAnnouncement(announcement, result);
        }

        return result;
    }

    /**
     * Validates oral exam announcements
     */
    public ValidationResult validateOralExamAnnouncements(List<OralExamAnnouncement> announcements) {
        ValidationResult result = new ValidationResult();

        if (announcements == null || announcements.isEmpty()) {
            // Oral exams are optional, so no error
            return result;
        }

        for (OralExamAnnouncement announcement : announcements) {
            validateOralAnnouncement(announcement, result);
        }

        return result;
    }

    /**
     * Validates a single written exam announcement
     */
    private void validateWrittenAnnouncement(WrittenExamAnnouncement announcement, ValidationResult result) {
        String context = "id=" + announcement.getId();

        // Required fields
        if (announcement.getId() == null) {
            result.addError("DATA", "Missing id", context);
        }
        if (announcement.getStudentId() == null) {
            result.addError("DATA", "Missing student_id", context);
        }
        if (announcement.getExamCode() == null || announcement.getExamCode().isEmpty()) {
            result.addError("DATA", "Missing exam_code", context);
        }
        if (announcement.getRoom() == null || announcement.getRoom().isEmpty()) {
            result.addError("DATA", "Missing room", context);
        }
        if (announcement.getSessionKey() == null || announcement.getSessionKey().isEmpty()) {
            result.addError("DATA", "Missing session_key", context);
        }

        // Room type validation
        if (announcement.getRoomType() != null) {
            if (!VALID_ROOM_TYPES.contains(announcement.getRoomType())) {
                result.addError("ROOM_TYPE",
                    "Invalid room_type: " + announcement.getRoomType() +
                    ". Must be one of: aud, drawing, pclab", context);
            }
        } else {
            result.addError("DATA", "Missing room_type", context);
        }

        // Date validation
        if (announcement.getExamDate() == null || announcement.getExamDate().isEmpty()) {
            result.addError("DATA", "Missing exam_date", context);
        }
    }

    /**
     * Validates a single oral exam announcement
     */
    private void validateOralAnnouncement(OralExamAnnouncement announcement, ValidationResult result) {
        String context = "id=" + announcement.getId();

        // Required fields
        if (announcement.getId() == null) {
            result.addError("DATA", "Missing id", context);
        }
        if (announcement.getStudentId() == null) {
            result.addError("DATA", "Missing student_id", context);
        }
        if (announcement.getExamCode() == null || announcement.getExamCode().isEmpty()) {
            result.addError("DATA", "Missing exam_code", context);
        }
        if (announcement.getRoom() == null || announcement.getRoom().isEmpty()) {
            result.addError("DATA", "Missing room", context);
        }
        if (announcement.getDay() == null || announcement.getDay() <= 0) {
            result.addError("DATA", "Missing or invalid day", context);
        }
    }

    /**
     * Validates question availability for written exam sessions
     * Groups announcements by session and validates sufficient questions exist
     */
    public ValidationResult validateQuestionAvailability(
            List<WrittenExamAnnouncement> announcements,
            Map<String, List<TaramaQuestion>> questionsBySession) {

        ValidationResult result = new ValidationResult();

        // Track missing questions by course-language
        Map<String, MissingQuestionInfo> missingByCourseLanguage = new LinkedHashMap<>();

        // Group announcements by session
        Map<String, List<WrittenExamAnnouncement>> sessionGroups = new HashMap<>();
        for (WrittenExamAnnouncement announcement : announcements) {
            String sessionKey = announcement.getSessionKey();
            sessionGroups.computeIfAbsent(sessionKey, k -> new ArrayList<>()).add(announcement);
        }

        // Validate each session
        for (Map.Entry<String, List<WrittenExamAnnouncement>> entry : sessionGroups.entrySet()) {
            String sessionKey = entry.getKey();
            List<WrittenExamAnnouncement> sessionAnnouncements = entry.getValue();
            int studentCount = sessionAnnouncements.size();

            // Get first announcement to extract course+language
            WrittenExamAnnouncement firstAnnouncement = sessionAnnouncements.get(0);
            String examCode = firstAnnouncement.getExamCode();
            String language = firstAnnouncement.getCurriculumLanguage();
            String courseLanguageKey = examCode + ":" + language;

            // Get unique question IDs for this session
            List<TaramaQuestion> questions = questionsBySession.getOrDefault(sessionKey, new ArrayList<>());
            Set<String> uniqueQuestionIds = new HashSet<>();
            for (TaramaQuestion q : questions) {
                uniqueQuestionIds.add(q.getId());
            }

            int availableQuestions = uniqueQuestionIds.size();

            if (availableQuestions == 0) {
                Integer day = firstAnnouncement.getDay();
                result.addError("QUESTIONS",
                    "No questions available for session",
                    "day=" + day, "session=" + sessionKey, "course=" + examCode, "language=" + language, "students=" + studentCount);

                // Track for course-language summary
                MissingQuestionInfo info = missingByCourseLanguage.get(courseLanguageKey);
                if (info == null) {
                    info = new MissingQuestionInfo(examCode, language);
                    missingByCourseLanguage.put(courseLanguageKey, info);
                }
                info.addSession(sessionKey, studentCount, day);

            } else if (availableQuestions == 1) {
                Integer day = firstAnnouncement.getDay();
                result.addWarning("QUESTIONS",
                    "Only 1 question available - all students will get same question",
                    "day=" + day, "session=" + sessionKey, "course=" + examCode, "language=" + language, "students=" + studentCount);
            }
            // N=2 is OK, no warning
            // N>=3 will use reserve logic in assignment
        }

        // Add summary of missing questions by course-language
        if (!missingByCourseLanguage.isEmpty()) {
            result.addError("MISSING_QUESTIONS_SUMMARY",
                "Missing questions for " + missingByCourseLanguage.size() + " course-language combinations");

            for (MissingQuestionInfo info : missingByCourseLanguage.values()) {
                String daysStr = info.getDaysString();
                result.addError("MISSING_QUESTIONS_DETAIL",
                    "Course: " + info.examCode + ", Language: " + info.language,
                    "days=" + daysStr, "sessions=" + info.sessionCount, "total_students=" + info.totalStudents);
            }
        }

        return result;
    }

    /**
     * Helper class to track missing questions by course-language
     */
    private static class MissingQuestionInfo {
        String examCode;
        String language;
        int sessionCount = 0;
        int totalStudents = 0;
        Set<Integer> affectedDays = new TreeSet<>();

        MissingQuestionInfo(String examCode, String language) {
            this.examCode = examCode;
            this.language = language;
        }

        void addSession(String sessionKey, int studentCount, Integer day) {
            this.sessionCount++;
            this.totalStudents += studentCount;
            if (day != null) {
                this.affectedDays.add(day);
            }
        }

        String getDaysString() {
            if (affectedDays.isEmpty()) {
                return "unknown";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Integer day : affectedDays) {
                if (count++ > 0) sb.append(",");
                sb.append(day);
            }
            return sb.toString();
        }
    }

    /**
     * Validates that image files exist in the exam images directory
     */
    public ValidationResult validateImageFiles(List<TaramaQuestion> questions) {
        ValidationResult result = new ValidationResult();

        Set<String> checkedFiles = new HashSet<>();

        for (TaramaQuestion question : questions) {
            String fileName = question.getFileName();

            if (fileName == null || fileName.isEmpty()) {
                result.addError("IMAGE_FILE",
                    "Question has null or empty image path",
                    "question_id=" + question.getId());
                continue;
            }

            // Skip if already checked
            if (checkedFiles.contains(fileName)) {
                continue;
            }
            checkedFiles.add(fileName);

            // Check if file exists
            Path filePath = Paths.get(examImagesDir, fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                result.addError("IMAGE_FILE",
                    "Image file not found: " + fileName,
                    "path=" + filePath.toString());
            } else if (!file.canRead()) {
                result.addError("IMAGE_FILE",
                    "Image file not readable: " + fileName,
                    "path=" + filePath.toString());
            } else if (file.length() == 0) {
                result.addWarning("IMAGE_FILE",
                    "Image file is empty: " + fileName,
                    "path=" + filePath.toString());
            }
        }

        return result;
    }

    /**
     * Combines multiple validation results
     */
    public ValidationResult combineResults(ValidationResult... results) {
        ValidationResult combined = new ValidationResult();

        for (ValidationResult result : results) {
            if (result != null) {
                for (ValidationResult.ValidationIssue error : result.getErrors()) {
                    combined.addError(error.getCategory(), error.getMessage(), error.getContext());
                }
                for (ValidationResult.ValidationIssue warning : result.getWarnings()) {
                    combined.addWarning(warning.getCategory(), warning.getMessage(), warning.getContext());
                }
            }
        }

        return combined;
    }
}
