-- ============================================================================
-- COMPREHENSIVE VALIDATION SCRIPT
-- Check if assigned questions match exam code and language requirements
-- ============================================================================

-- Summary Report
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
SELECT 'VALIDATION SUMMARY REPORT' AS report_title;
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;

SELECT
    'Total Assignments' AS check_type,
    COUNT(*) AS count
FROM kexamprint.question_assignments

UNION ALL

SELECT
    'Exam Code Mismatches',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
WHERE wea.exam_code != qa.exam_code

UNION ALL

SELECT
    'Language Mismatches',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
WHERE wea.curriculum_language != qa.curriculum_language

UNION ALL

SELECT
    'Questions Not Found in Tarama',
    COUNT(*)
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t ON t.id = qa.tarama_question_id
WHERE t.id IS NULL

UNION ALL

SELECT
    'Tarama Exam Code Mismatches',
    COUNT(*)
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t ON t.id = qa.tarama_question_id
WHERE t.id IS NOT NULL AND t.derskodu != qa.exam_code

UNION ALL

SELECT
    'Tarama Language Mismatches',
    COUNT(*)
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t ON t.id = qa.tarama_question_id
WHERE t.id IS NOT NULL AND t.dersdili != qa.curriculum_language;

-- Detail: Assignment vs Announcement Mismatches
SELECT '';
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
SELECT 'ASSIGNMENT vs ANNOUNCEMENT MISMATCHES' AS section_title;
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;

SELECT
    qa.id AS assignment_id,
    qa.student_id,
    qa.placement_id,
    wea.exam_code AS announced_exam_code,
    qa.exam_code AS assigned_exam_code,
    wea.curriculum_language AS announced_language,
    qa.curriculum_language AS assigned_language,
    qa.tarama_question_id,
    wea.room,
    wea.seat_no,
    wea.exam_date,
    wea.start_time,
    CASE
        WHEN wea.exam_code != qa.exam_code THEN 'EXAM_CODE_MISMATCH'
        WHEN wea.curriculum_language != qa.curriculum_language THEN 'LANGUAGE_MISMATCH'
        ELSE 'OK'
    END AS validation_status
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
WHERE wea.exam_code != qa.exam_code
   OR wea.curriculum_language != qa.curriculum_language
ORDER BY validation_status, qa.student_id
LIMIT 50;

-- Detail: Tarama Question Validation
SELECT '';
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
SELECT 'TARAMA QUESTION VALIDATION' AS section_title;
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;

SELECT
    qa.id AS assignment_id,
    qa.student_id,
    qa.exam_code AS assigned_exam,
    qa.curriculum_language AS assigned_lang,
    qa.tarama_question_id,
    t.id AS tarama_id,
    t.derskodu AS tarama_exam,
    t.dersdili AS tarama_lang,
    CASE
        WHEN t.id IS NULL THEN 'QUESTION_NOT_FOUND'
        WHEN t.derskodu != qa.exam_code THEN 'TARAMA_EXAM_MISMATCH'
        WHEN t.dersdili != qa.curriculum_language THEN 'TARAMA_LANGUAGE_MISMATCH'
        ELSE 'OK'
    END AS tarama_validation
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t ON t.id = qa.tarama_question_id
WHERE t.id IS NULL
   OR t.derskodu != qa.exam_code
   OR t.dersdili != qa.curriculum_language
ORDER BY tarama_validation, qa.student_id
LIMIT 50;

-- Sample of Correct Assignments
SELECT '';
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
SELECT 'SAMPLE CORRECT ASSIGNMENTS (First 10)' AS section_title;
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;

SELECT
    qa.student_id,
    qa.exam_code,
    qa.curriculum_language,
    qa.tarama_question_id,
    wea.exam_code AS wea_exam,
    wea.curriculum_language AS wea_lang,
    t.derskodu AS tarama_exam,
    t.dersdili AS tarama_lang,
    'ALL_MATCH' AS status
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN vg12526.tarama t ON t.id = qa.tarama_question_id
WHERE wea.exam_code = qa.exam_code
  AND wea.curriculum_language = qa.curriculum_language
  AND t.derskodu = qa.exam_code
  AND t.dersdili = qa.curriculum_language
LIMIT 10;

SELECT '';
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
SELECT 'VALIDATION COMPLETE' AS end_message;
SELECT '=' AS separator, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5, '=' AS sep6;
