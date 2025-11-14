-- ============================================================================
-- QUESTION ID VALIDATION SCRIPT
-- Verify that tarama_question_id and question_id both match the assignment
-- ============================================================================

SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'QUESTION ID VALIDATION REPORT' AS title;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

-- Summary Statistics
SELECT
    'Total Assignments' AS check_type,
    COUNT(*) AS count
FROM kexamprint.question_assignments

UNION ALL

SELECT
    'Assignments with NULL question_id',
    COUNT(*)
FROM kexamprint.question_assignments
WHERE question_id IS NULL

UNION ALL

SELECT
    'Assignments with NULL tarama_question_id',
    COUNT(*)
FROM kexamprint.question_assignments
WHERE tarama_question_id IS NULL

UNION ALL

SELECT
    'tarama_question_id NOT found in tarama table',
    COUNT(*)
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
WHERE t1.id IS NULL

UNION ALL

SELECT
    'question_id NOT found in tarama table',
    COUNT(*)
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t2.real_id IS NULL

UNION ALL

SELECT
    'tarama_question_id exam_code MISMATCH',
    COUNT(*)
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
WHERE t1.derskodu != qa.exam_code

UNION ALL

SELECT
    'question_id exam_code MISMATCH',
    COUNT(*)
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t2.derskodu != qa.exam_code

UNION ALL

SELECT
    'tarama_question_id language MISMATCH',
    COUNT(*)
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
WHERE t1.dersdili != qa.curriculum_language

UNION ALL

SELECT
    'question_id language MISMATCH',
    COUNT(*)
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t2.dersdili != qa.curriculum_language

UNION ALL

SELECT
    'Both IDs point to DIFFERENT tarama records',
    COUNT(*)
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t1.id != t2.id;

-- Detailed Analysis: Check if tarama_question_id and question_id match
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'DETAILED: tarama_question_id vs question_id Consistency' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    qa.id AS assignment_id,
    qa.student_id,
    qa.exam_code AS assigned_exam,
    qa.curriculum_language AS assigned_lang,
    qa.tarama_question_id,
    qa.question_id,
    t1.id AS tarama_via_id,
    t1.real_id AS tarama_real_id,
    t2.id AS tarama_via_real_id,
    t2.real_id AS via_real_id_check,
    t1.derskodu AS t1_exam,
    t1.dersdili AS t1_lang,
    t2.derskodu AS t2_exam,
    t2.dersdili AS t2_lang,
    CASE
        WHEN t1.id IS NULL THEN 'tarama_question_id NOT FOUND'
        WHEN t2.real_id IS NULL THEN 'question_id NOT FOUND'
        WHEN t1.id != t2.id THEN 'IDs POINT TO DIFFERENT RECORDS'
        WHEN t1.derskodu != qa.exam_code THEN 'tarama_question_id EXAM MISMATCH'
        WHEN t2.derskodu != qa.exam_code THEN 'question_id EXAM MISMATCH'
        WHEN t1.dersdili != qa.curriculum_language THEN 'tarama_question_id LANG MISMATCH'
        WHEN t2.dersdili != qa.curriculum_language THEN 'question_id LANG MISMATCH'
        ELSE 'OK'
    END AS status
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
LEFT JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t1.id IS NULL
   OR t2.real_id IS NULL
   OR t1.id != t2.id
   OR t1.derskodu != qa.exam_code
   OR t2.derskodu != qa.exam_code
   OR t1.dersdili != qa.curriculum_language
   OR t2.dersdili != qa.curriculum_language
ORDER BY status, qa.id
LIMIT 50;

-- Sample of correct assignments
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'SAMPLE: Correct Assignments (First 10)' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    qa.id AS assignment_id,
    qa.student_id,
    qa.exam_code,
    qa.curriculum_language,
    qa.tarama_question_id,
    qa.question_id,
    t1.id AS t1_id,
    t1.real_id AS t1_real_id,
    t1.derskodu AS t1_exam,
    t1.dersdili AS t1_lang,
    'ALL_MATCH' AS status
FROM kexamprint.question_assignments qa
JOIN vg12526.tarama t1 ON qa.tarama_question_id = t1.id
JOIN vg12526.tarama t2 ON qa.question_id = t2.real_id
WHERE t1.id = t2.id
  AND t1.derskodu = qa.exam_code
  AND t1.dersdili = qa.curriculum_language
  AND t2.derskodu = qa.exam_code
  AND t2.dersdili = qa.curriculum_language
LIMIT 10;

SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'VALIDATION COMPLETE' AS end_message;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
