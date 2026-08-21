# 첫출근 더미데이터 v1

> **참고용 스냅샷 — 현재 스키마에서는 실행되지 않는다.**
>
> V1 시점 기준으로 작성했고 이후 마이그레이션을 반영하지 않았다. 어떤 계정·상태 조합을
> 테스트용으로 설계했는지 참고하는 용도로만 둔다. 실행하려면 아래 2곳을 먼저 고쳐야 한다.
>
> | 파일 | 문제 | 원인 |
> |---|---|---|
> | `01_accounts_and_masters.sql` — `skills` | `skill_group` 컬럼이 없어졌고 `depth`(NOT NULL)가 빠져 있다 | V2·V3에서 `parent_id`/`depth` 계층 구조로 변경 |
> | `02_jobs_and_documents.sql` — `cover_letter_ai_reviews` | `job_posting_id`(NOT NULL)가 빠져 있다 | V11에서 추가 |
>
> 각 파일이 `START TRANSACTION` ~ `COMMIT`으로 묶여 있어, `skills`에서 실패하면
> **01 파일 전체가 롤백된다.** 앞쪽 계정·기업 데이터까지 들어가지 않으니 주의할 것.
> `02`의 `job_posting_skills`·`resume_skills`도 `skill_id`를 참조하므로 연쇄로 실패한다.
>
> 그 외 테이블의 INSERT는 현재 DDL과 일치한다. `companies.latest_rejection_code`(V4)는
> 빠져 있지만 nullable이라 실행에는 지장이 없다. 다만 이 더미의 반려 기업(`company08`)은
> 코드가 NULL이라 반려 화면에서 "확인할 항목" 안내와 필드 강조가 표시되지 않는다.

기준 스키마: `firstday_mysql_ddl_v1(3)(4).sql` (MySQL 8.0.16+)

## 실행 전제

- 반드시 DDL을 먼저 실행해 `firstday` 데이터베이스와 37개 테이블을 생성한다.
- 아래 SQL은 **비어 있는 개발·시연 DB에 최초 1회 실행**하는 초기 데이터다.
- 모든 계정의 `password_hash`에는 다음 BCrypt 문자열을 동일하게 저장한다.
  - `$2a$10$QdCzi0m/Og9hxaw7owfiMeUajYNJFl7JomOp/8qBYnHQiSgnTkAMW`
- BCrypt는 복호화할 수 없으므로 실제 로그인 비밀번호는 이 해시를 만들 때 사용한 원문과 일치해야 한다.

## 파일 분리 기준과 실행 순서

1. `01_accounts_and_masters.sql`
   - 관리자·개인·기업 계정
   - 승인·반려·이용정지·탈퇴 기업
   - 개인 프로필, 약관·동의
   - 직무 카테고리, 기술 스택, 희망 직무
2. `02_jobs_and_documents.sql`
   - 채용공고와 공고 기술
   - 관심 공고·관심 기업
   - 이력서·학력·경력·프로젝트·보유 기술
   - 자기소개서와 AI 첨삭 결과
3. `03_applications_and_reviews.sql`
   - 전체 지원 상태별 입사지원
   - 지원 상태 이력과 기업 내부 메모
   - 기업리뷰·면접후기·도움돼요
   - 연봉정보
4. `04_customer_service_and_operation.sql`
   - 공지, FAQ, 1:1 문의·첨부 메타데이터
   - 신고 처리 사례
   - 배너, 사이트 설정, 사이트 버전

각 파일은 `START TRANSACTION`과 `COMMIT`으로 묶여 있다. 중간 파일이 실패하면 해당 파일만 롤백되며, 오류를 수정한 뒤 그 파일부터 다시 실행할 수 있다.

## 대표 로그인 아이디

| 구분 | 로그인 아이디 | 상태/용도 |
|---|---|---|
| 관리자 | `admin` | 관리자 전체 화면 |
| 개인회원 | `personal01` | 이력서·지원·기업리뷰 보유 |
| 개인회원 | `personal03` | 지원완료 상태 |
| 개인회원 | `personal09` | 지원취소 상태 |
| 개인회원 | `personal11` | 이용정지 계정 |
| 기업회원 | `company01` | 코드웨이브, 승인·정상 |
| 기업회원 | `company07` | 승인대기 기업 |
| 기업회원 | `company08` | 반려 기업 |
| 기업회원 | `company09` | 이용정지 기업 |
| 기업회원 | `company10` | 탈퇴 기업 |

## PostgreSQL(pgvector)를 별도 SQL로 만들지 않은 이유

`ai.vector_store.embedding`은 `qwen3-embedding:0.6b`가 생성한 1024차원 벡터다. 임의의 0 벡터나 가짜 수치를 넣으면 유사도 검색 결과가 무의미해진다. MySQL 더미 원문을 적재한 뒤 Spring AI의 실제 임베딩 적재 로직으로 PostgreSQL 데이터를 생성한다.

## 주의

- `inquiry_attachments`에는 S3 객체의 메타데이터만 들어간다. 예시 `storage_key`와 동일한 객체를 실제 team2 S3 버킷에 업로드하지 않으면 다운로드는 실패한다.
- 이미지 URL은 화면 확인을 위한 외부 플레이스홀더다. 실제 배포 전에는 S3/CloudFront URL로 교체한다.
- 이 파일들은 기존 데이터를 삭제하지 않는다. 같은 PK·아이디·이메일이 이미 있으면 중복키 오류가 발생한다.
