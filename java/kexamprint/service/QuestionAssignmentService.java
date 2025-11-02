package kexamprint.service;

import kexamprint.model.*;
import kexamprint.util.ResourceLoader;

import java.util.*;

/**
 * Service for assigning questions to students using round-robin algorithm
 */
public class QuestionAssignmentService {

    private static final int MIN_QUESTIONS_FOR_RESERVE = 3;
    private final int questionsToReserve;

    public QuestionAssignmentService() {
        // Load reserve count from config, default to 1
        this.questionsToReserve = ResourceLoader.getConfigInt("question.reserve.count", 1);
    }

    /**
     * Assigns questions to students for a session using round-robin algorithm
     *
     * @param announcements Students in the session (ordered by seat_no)
     * @param availableQuestions All available questions for this exam/language
     * @return List of question assignments
     */
    public List<QuestionAssignment> assignQuestionsForSession(
            List<WrittenExamAnnouncement> announcements,
            List<TaramaQuestion> availableQuestions) {

        if (announcements == null || announcements.isEmpty()) {
            throw new IllegalArgumentException("No announcements provided");
        }

        if (availableQuestions == null || availableQuestions.isEmpty()) {
            throw new IllegalArgumentException("No questions available");
        }

        // Get unique question IDs
        List<String> uniqueQuestionIds = getUniqueQuestionIds(availableQuestions);
        int totalQuestions = uniqueQuestionIds.size();
        int studentCount = announcements.size();

        // Determine how many questions to use (apply reserve logic)
        int questionsToUse = calculateQuestionsToUse(totalQuestions, studentCount);

        // Select questions to use (first N questions)
        List<String> questionsPool = uniqueQuestionIds.subList(0, questionsToUse);

        // Assign questions using round-robin
        return assignQuestionsRoundRobin(announcements, questionsPool);
    }

    /**
     * Gets unique question IDs from the list of questions
     */
    private List<String> getUniqueQuestionIds(List<TaramaQuestion> questions) {
        Set<String> uniqueIds = new LinkedHashSet<>();  // Preserve order
        for (TaramaQuestion question : questions) {
            uniqueIds.add(question.getId());
        }
        return new ArrayList<>(uniqueIds);
    }

    /**
     * Calculates how many questions to use based on reserve logic:
     * - N=0: ERROR (should be caught in validation)
     * - N=1,2: Use all questions (no reserve)
     * - N>=3: Reserve some questions, use N-reserve
     *
     * Note: With round-robin, we cycle through questions, so we don't need
     * questionsToUse >= studentCount. Even 1 question can serve all students.
     */
    private int calculateQuestionsToUse(int totalQuestions, int studentCount) {
        if (totalQuestions < MIN_QUESTIONS_FOR_RESERVE) {
            // Use all available questions
            return totalQuestions;
        }

        // Reserve questions
        int questionsToUse = totalQuestions - questionsToReserve;

        // Ensure we have at least 1 question to use
        if (questionsToUse < 1) {
            throw new IllegalStateException(
                String.format("After reserve, no questions left to use: total=%d, reserve=%d",
                    totalQuestions, questionsToReserve));
        }

        return questionsToUse;
    }

    /**
     * Assigns questions to students using simple round-robin algorithm
     * This minimizes adjacent students getting the same question
     */
    private List<QuestionAssignment> assignQuestionsRoundRobin(
            List<WrittenExamAnnouncement> announcements,
            List<String> questionsPool) {

        List<QuestionAssignment> assignments = new ArrayList<>();
        int poolSize = questionsPool.size();

        for (int i = 0; i < announcements.size(); i++) {
            WrittenExamAnnouncement announcement = announcements.get(i);

            // Round-robin: cycle through questions
            String assignedQuestionId = questionsPool.get(i % poolSize);

            // Create assignment
            QuestionAssignment assignment = new QuestionAssignment(
                announcement.getPlacementId(),
                announcement.getStudentId(),
                announcement.getRoomCode(),
                announcement.getExamCode(),
                announcement.getCurriculumLanguage(),
                assignedQuestionId,
                announcement.getSessionKey()
            );

            assignments.add(assignment);
        }

        return assignments;
    }

    /**
     * Groups announcements by session key
     */
    public Map<String, List<WrittenExamAnnouncement>> groupBySession(
            List<WrittenExamAnnouncement> announcements) {

        Map<String, List<WrittenExamAnnouncement>> groups = new LinkedHashMap<>();

        for (WrittenExamAnnouncement announcement : announcements) {
            String sessionKey = announcement.getSessionKey();
            groups.computeIfAbsent(sessionKey, k -> new ArrayList<>()).add(announcement);
        }

        // Sort each group by seat number
        for (List<WrittenExamAnnouncement> group : groups.values()) {
            group.sort(Comparator.comparing(WrittenExamAnnouncement::getSeatNo,
                Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return groups;
    }

    /**
     * Groups questions by exam code and language
     */
    public Map<String, List<TaramaQuestion>> groupQuestionsByExamAndLanguage(
            List<TaramaQuestion> questions) {

        Map<String, List<TaramaQuestion>> groups = new HashMap<>();

        for (TaramaQuestion question : questions) {
            String key = makeQuestionGroupKey(question.getExamCode(), question.getLanguage());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(question);
        }

        return groups;
    }

    /**
     * Creates a key for grouping questions by exam and language
     */
    public String makeQuestionGroupKey(String examCode, String language) {
        return examCode + ":" + language;
    }

    /**
     * Gets the question group key for an announcement
     */
    public String getQuestionGroupKeyForAnnouncement(WrittenExamAnnouncement announcement) {
        return makeQuestionGroupKey(announcement.getExamCode(), announcement.getCurriculumLanguage());
    }
}
