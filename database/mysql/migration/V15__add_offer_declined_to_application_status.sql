-- =========================================================
-- FirstDay MySQL Migration V15
-- applications.current_status CHECK 허용값에 '입사포기'를 추가한다.
-- 최종합격 후 입사를 포기하는 경우를 입사완료·불합격과 구분해서
-- 표현할 수 있게 한다.
-- 적용 대상: V14까지 적용된 기존 MySQL DB
-- =========================================================

ALTER TABLE applications
    DROP CHECK chk_applications_status;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_status
    CHECK (
        current_status IN (
            '지원완료',
            '서류검토중',
            '서류합격',
            '면접예정',
            '면접완료',
            '최종합격',
            '입사완료',
            '입사포기',
            '불합격',
            '지원취소',
            '채용종료'
        )
    );

-- 적용 확인
SHOW CREATE TABLE applications;
