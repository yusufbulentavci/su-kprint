-- ============================================================================
-- PAPER CODE VALIDATION SCRIPT
-- Validates that paper_code assignments match historical data and question attributes
-- Uses session+seat matching: start_time, end_time, room, seat_no
-- ============================================================================

SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'PAPER CODE VALIDATION REPORT' AS title;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

-- Summary Statistics
SELECT
    'Total Assignments with paper_code' AS check_type,
    COUNT(*) AS count
FROM kexamprint.question_assignments
WHERE paper_code IS NOT NULL

UNION ALL

SELECT
    'Matched with historical data',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN kexamprint.written_exam_full_data fd
    ON wea.start_time = fd.start_time
    AND wea.end_time = fd.end_time
    AND wea.room = fd.room
    AND wea.seat_no = fd.seat_no
WHERE qa.paper_code IS NOT NULL

UNION ALL

SELECT
    'Assignment exam_code vs Announcement mismatch',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
WHERE qa.exam_code != wea.exam_code

UNION ALL

SELECT
    'Assignment vs Historical exam_code mismatch',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN kexamprint.written_exam_full_data fd
    ON wea.start_time = fd.start_time
    AND wea.end_time = fd.end_time
    AND wea.room = fd.room
    AND wea.seat_no = fd.seat_no
WHERE qa.exam_code != fd.exam_code

UNION ALL

SELECT
    'Assignment vs Historical language mismatch',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN kexamprint.written_exam_full_data fd
    ON wea.start_time = fd.start_time
    AND wea.end_time = fd.end_time
    AND wea.room = fd.room
    AND wea.seat_no = fd.seat_no
WHERE qa.curriculum_language != fd.curriculum_language

UNION ALL

SELECT
    'Question (tarama) exam_code mismatch',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN vg12526.tarama t ON qa.question_id = t.real_id
WHERE t.derskodu != qa.exam_code

UNION ALL

SELECT
    'Question (tarama) language mismatch',
    COUNT(*)
FROM kexamprint.question_assignments qa
INNER JOIN vg12526.tarama t ON qa.question_id = t.real_id
WHERE t.dersdili != qa.curriculum_language;

-- Detail: Mismatches between Assignment and Historical Data
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'ASSIGNMENT vs HISTORICAL DATA MISMATCHES' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    qa.paper_code,
    qa.student_id,
    wea.room,
    wea.seat_no,
    wea.start_time,
    wea.end_time,
    qa.exam_code AS assigned_exam,
    fd.exam_code AS historical_exam,
    qa.curriculum_language AS assigned_lang,
    fd.curriculum_language AS historical_lang,
    CASE
        WHEN qa.exam_code != fd.exam_code THEN 'EXAM_CODE_MISMATCH'
        WHEN qa.curriculum_language != fd.curriculum_language THEN 'LANGUAGE_MISMATCH'
        ELSE 'OK'
    END AS validation_status
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN kexamprint.written_exam_full_data fd
    ON wea.start_time = fd.start_time
    AND wea.end_time = fd.end_time
    AND wea.room = fd.room
    AND wea.seat_no = fd.seat_no
WHERE qa.exam_code != fd.exam_code
   OR qa.curriculum_language != fd.curriculum_language
ORDER BY validation_status, qa.paper_code
LIMIT 50;

-- Detail: Question Assignment Validation (with tarama)
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'QUESTION (TARAMA) VALIDATION' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    qa.paper_code,
    qa.student_id,
    qa.exam_code AS assigned_exam,
    qa.curriculum_language AS assigned_lang,
    qa.question_id AS real_id,
    t.id AS tarama_id,
    t.derskodu AS question_exam,
    t.dersdili AS question_lang,
    CASE
        WHEN t.derskodu != qa.exam_code THEN 'QUESTION_EXAM_MISMATCH'
        WHEN t.dersdili != qa.curriculum_language THEN 'QUESTION_LANG_MISMATCH'
        ELSE 'OK'
    END AS validation_status
FROM kexamprint.question_assignments qa
LEFT JOIN vg12526.tarama t ON qa.question_id = t.real_id
WHERE t.derskodu != qa.exam_code
   OR t.dersdili != qa.curriculum_language
ORDER BY validation_status, qa.paper_code
LIMIT 50;

-- Sample of Correct Assignments (Full Chain Validation)
SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'SAMPLE: Fully Validated Assignments (First 10)' AS section;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;

SELECT
    qa.paper_code,
    qa.student_id,
    wea.room,
    wea.seat_no,
    qa.exam_code,
    qa.curriculum_language,
    qa.question_id,
    t.id AS tarama_id,
    'ALL_VALIDATED' AS status
FROM kexamprint.question_assignments qa
INNER JOIN kexam.written_exam_announcements wea ON qa.placement_id = wea.id
INNER JOIN kexamprint.written_exam_full_data fd
    ON wea.start_time = fd.start_time
    AND wea.end_time = fd.end_time
    AND wea.room = fd.room
    AND wea.seat_no = fd.seat_no
INNER JOIN vg12526.tarama t ON qa.question_id = t.real_id
WHERE qa.exam_code = wea.exam_code
  AND qa.exam_code = fd.exam_code
  AND qa.exam_code = t.derskodu
  AND qa.curriculum_language = wea.curriculum_language
  AND qa.curriculum_language = fd.curriculum_language
  AND qa.curriculum_language = t.dersdili
LIMIT 10;

SELECT '';
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
SELECT 'VALIDATION COMPLETE' AS end_message;
SELECT '=' AS sep, '=' AS sep2, '=' AS sep3, '=' AS sep4, '=' AS sep5;
