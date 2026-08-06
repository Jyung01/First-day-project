# MySQL 데이터베이스

첫출근 서비스의 회원, 기업, 공고, 지원, 후기, 고객센터, 관리자 업무 데이터를 저장한다.

## 폴더

- `ddl/`: 모든 변경이 반영된 최신 전체 DDL
- `dummy-data/`: 기준 데이터와 시연용 데이터
- `migration/`: 기존 DB에 순서대로 적용하는 변경 SQL

## 현재 버전

- 최신 변경 버전: V6
- 신규 DB 생성: `ddl/firstday_mysql_current.sql` 실행
- V5 DB 업데이트: `migration/V6__add_scheduled_job_posting_status.sql` 실행

## V6 핵심 변경

채용공고 임시저장을 위해 `job_postings`의 직무 카테고리, 고용형태, 경력, 학력, 근무지역, 모집인원, 주요업무, 자격요건을 NULL 허용으로 변경했다.

- `job_postings.status` CHECK 허용값에 `모집예정`을 추가한다.
