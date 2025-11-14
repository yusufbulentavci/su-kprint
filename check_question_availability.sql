-- ============================================================================
-- QUESTION AVAILABILITY CHECK FOR PRODUCTION EXAMS
-- Check if we have unused questions for all exam-language combinations
-- ============================================================================

SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'QUESTION AVAILABILITY REPORT' AS title;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

-- Summary: Question availability per exam-language combination
WITH exam_requirements AS (
    SELECT DISTINCT
        exam_code,
        language,
        COUNT(*) as students_count
    FROM kexamprint.production_exam_schedule
    GROUP BY exam_code, language
),
available_questions AS (
    SELECT
        t.derskodu as exam_code,
        t.dersdili as language,
        COUNT(*) as total_questions,
        COUNT(*) FILTER (WHERE t.used_count = 0 OR t.used_count IS NULL) as unused_questions
    FROM vg12526.tarama t
    WHERE t.images IS NOT NULL
    GROUP BY t.derskodu, t.dersdili
)
SELECT
    er.exam_code,
    er.language,
    er.students_count,
    COALESCE(aq.total_questions, 0) as total_questions,
    COALESCE(aq.unused_questions, 0) as unused_questions,
    CASE
        WHEN COALESCE(aq.unused_questions, 0) = 0 THEN 'NO_QUESTIONS'
        WHEN COALESCE(aq.unused_questions, 0) < er.students_count THEN 'INSUFFICIENT'
        ELSE 'OK'
    END as status
FROM exam_requirements er
LEFT JOIN available_questions aq
    ON er.exam_code = aq.exam_code
    AND er.language = aq.language
ORDER BY
    CASE
        WHEN COALESCE(aq.unused_questions, 0) = 0 THEN 1
        WHEN COALESCE(aq.unused_questions, 0) < er.students_count THEN 2
        ELSE 3
    END,
    er.exam_code,
    er.language;

-- Summary statistics
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'SUMMARY STATISTICS' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

WITH exam_requirements AS (
    SELECT DISTINCT
        exam_code,
        language,
        COUNT(*) as students_count
    FROM kexamprint.production_exam_schedule
    GROUP BY exam_code, language
),
available_questions AS (
    SELECT
        t.derskodu as exam_code,
        t.dersdili as language,
        COUNT(*) as total_questions,
        COUNT(*) FILTER (WHERE t.used_count = 0 OR t.used_count IS NULL) as unused_questions
    FROM vg12526.tarama t
    WHERE t.images IS NOT NULL
    GROUP BY t.derskodu, t.dersdili
),
availability_check AS (
    SELECT
        er.exam_code,
        er.language,
        er.students_count,
        COALESCE(aq.unused_questions, 0) as unused_questions,
        CASE
            WHEN COALESCE(aq.unused_questions, 0) = 0 THEN 'NO_QUESTIONS'
            WHEN COALESCE(aq.unused_questions, 0) < er.students_count THEN 'INSUFFICIENT'
            ELSE 'OK'
        END as status
    FROM exam_requirements er
    LEFT JOIN available_questions aq
        ON er.exam_code = aq.exam_code
        AND er.language = aq.language
)
SELECT
    'Total exam-language combinations needed' AS metric,
    COUNT(*)::TEXT AS value
FROM availability_check

UNION ALL

SELECT
    'Combinations with NO questions',
    COUNT(*)::TEXT
FROM availability_check
WHERE status = 'NO_QUESTIONS'

UNION ALL

SELECT
    'Combinations with INSUFFICIENT questions',
    COUNT(*)::TEXT
FROM availability_check
WHERE status = 'INSUFFICIENT'

UNION ALL

SELECT
    'Combinations with enough questions (OK)',
    COUNT(*)::TEXT
FROM availability_check
WHERE status = 'OK'

UNION ALL

SELECT
    'Total students needing questions',
    SUM(students_count)::TEXT
FROM availability_check;

SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'CHECK COMPLETE' AS end_message;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
