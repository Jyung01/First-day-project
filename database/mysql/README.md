# MySQL 데이터베이스

첫출근 서비스의 회원, 기업, 공고, 지원, 후기, 고객센터, 관리자 업무 데이터를 저장한다.

## 폴더

- `ddl/`: 모든 변경이 반영된 최신 전체 DDL
- `dummy-data/`: 기준 데이터와 시연용 데이터
- `migration/`: 기존 DB에 순서대로 적용하는 변경 SQL

## 현재 버전

- 최신 변경 버전: V10
- 신규 DB 생성: `ddl/firstday_mysql_current.sql` 실행
- V9 DB 업데이트: `migration/V10__add_admin_memo_to_interview_reviews.sql` 실행

## V6 핵심 변경

채용공고 임시저장을 위해 `job_postings`의 직무 카테고리, 고용형태, 경력, 학력, 근무지역, 모집인원, 주요업무, 자격요건을 NULL 허용으로 변경했다.

- `job_postings.status` CHECK 허용값에 `모집예정`을 추가한다.

## V7 핵심 변경

`applications`에 지원 시 첨부한 자기소개서 원본 참조와 지원 완료 시점의 스냅샷을 추가했다.

- `cover_letter_id`(FK, `ON DELETE SET NULL`), `cover_letter_snapshot_json` 추가

## V8 핵심 변경

`applications`의 지원 유일성 제약을 지원취소 건은 제외하도록 변경했다.

- `active_application_guard` 생성 컬럼 기반 `uk_applications_active`로 UNIQUE 교체, 지원취소 후 같은 공고 재지원 가능

## V9 핵심 변경

- `company_reviews.admin_memo` 추가 — 관리자 후기 처리 메모

## V10 핵심 변경

- `interview_reviews.admin_memo` 추가 — 관리자 후기 처리 메모
