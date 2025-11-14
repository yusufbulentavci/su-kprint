-- ============================================================================
-- V1 vs V2 CONSISTENCY VALIDATION
-- Compare written_exam_full_data (v1) with written_exam_print_data_v2 (v2)
-- ============================================================================

SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'V1 vs V2 CONSISTENCY REPORT' AS title;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

-- Summary Statistics
SELECT
    'Total records in V1' AS check_type,
    COUNT(*) AS count
FROM kexamprint.written_exam_full_data

UNION ALL

SELECT
    'Total records in V2',
    COUNT(*)
FROM kexamprint.written_exam_print_data_v2

UNION ALL

SELECT
    'V2 records with assignment',
    COUNT(*)
FROM kexamprint.written_exam_print_data_v2
WHERE assignment_id IS NOT NULL

UNION ALL

SELECT
    'Matched records (student+exam+date)',
    COUNT(*)
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_code = v2.exam_code
    AND v1.exam_date = v2.exam_date

UNION ALL

SELECT
    'Tarama ID mismatches',
    COUNT(*)
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_code = v2.exam_code
    AND v1.exam_date = v2.exam_date
WHERE SPLIT_PART(v1.question_seat, '-', 1) != v2.tarama_question_id

UNION ALL

SELECT
    'Exam code mismatches',
    COUNT(*)
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_date = v2.exam_date
WHERE v1.exam_code != v2.exam_code

UNION ALL

SELECT
    'Language mismatches',
    COUNT(*)
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_code = v2.exam_code
    AND v1.exam_date = v2.exam_date
WHERE v1.curriculum_language != v2.curriculum_language;

-- Detail: Tarama ID Mismatches
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'DETAIL: TARAMA_QUESTION_ID MISMATCHES' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    v1.student_id,
    v1.exam_code,
    v1.exam_date,
    v1.room,
    v1.seat_no,
    SPLIT_PART(v1.question_seat, '-', 1) AS v1_tarama_id,
    v2.tarama_question_id AS v2_tarama_id,
    v2.paper_code,
    'MISMATCH' AS status
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_code = v2.exam_code
    AND v1.exam_date = v2.exam_date
WHERE SPLIT_PART(v1.question_seat, '-', 1) != v2.tarama_question_id
ORDER BY v1.student_id, v1.exam_code
LIMIT 50;

-- Sample: Correct Matches
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'SAMPLE: CORRECT MATCHES (First 10)' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    v1.student_id,
    v1.exam_code,
    v1.curriculum_language,
    v1.exam_date,
    v1.room,
    v1.seat_no,
    SPLIT_PART(v1.question_seat, '-', 1) AS v1_tarama_id,
    v2.tarama_question_id AS v2_tarama_id,
    v2.question_id,
    v2.paper_code,
    'MATCH' AS status
FROM kexamprint.written_exam_full_data v1
INNER JOIN kexamprint.written_exam_print_data_v2 v2
    ON v1.student_id = v2.student_id
    AND v1.exam_code = v2.exam_code
    AND v1.exam_date = v2.exam_date
WHERE SPLIT_PART(v1.question_seat, '-', 1) = v2.tarama_question_id
  AND v1.curriculum_language = v2.curriculum_language
LIMIT 10;

SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'VALIDATION COMPLETE' AS end_message;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
