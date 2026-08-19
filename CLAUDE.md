# 첫출근 프로젝트 — Claude Code 컨텍스트

이 문서는 Claude Code가 이 저장소에서 작업할 때 참고하는 프로젝트 개요입니다.
민감정보(DB 접속정보, API 키 등)는 여기 적지 않습니다 — 모두 환경변수(`.env`)로 관리합니다.

## 1. 프로젝트 개요

- **이름**: 첫출근 — 구직자와 기업을 연결하는 AI 기반 채용 플랫폼
- **개발 기간**: 2026.07.20 ~ 2026.09.03
- **구성**: 개인회원 / 기업회원 / 관리자 3개 서비스로 분리
- **핵심 특징**: 실제 채용 프로세스를 반영한 입사지원·채용관리 + Spring AI(OpenAI) 기반 AI 보조 기능

## 2. 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | HTML5, CSS3, JavaScript, Thymeleaf |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, MyBatis, Spring AI, Gradle |
| Database | MySQL(업무 데이터, 원본), PostgreSQL + pgvector(임베딩 검색 전용 파생 데이터) |
| AI | OpenAI — chat: `gpt-5-mini`, embedding: `text-embedding-3-small` (dimension 1024) |
| Infra | AWS EC2, Amazon S3, GitHub Actions(적용 예정) |

**원칙**: PostgreSQL은 임베딩 검색용 파생 데이터만 저장. 회원·기업·공고·지원의 원본은 MySQL이며, **DB 간 외래키는 만들지 않는다.**

## 3. 담당 역할

| 이름 | 담당 영역 |
|---|---|
| 양지웅 | 회원 · 인증 · 권한 · 이력서 · 기업정보 · 기업 승인 · **자기소개서 AI 첨삭 로직(RAG)** |
| 오수현 | 채용공고 · 입사지원 · 지원자 관리 · 채용공고 AI 문장 다듬기 |
| 최수빈 | 메인 · 기업 조회 · 후기 · 연봉 · 관심 · 신고 · 배너 · 희망직무 기반 맞춤 추천 |
| 강현주 | 자기소개서(CRUD·결과저장) · 고객센터 · 사이트 운영 설정 |

> 자기소개서는 강현주 담당이 기본이지만, RAG 기반 AI 첨삭 로직(REQ-902)은 난이도상 양지웅이 담당합니다.

## 4. PostgreSQL / pgvector 설정 — 중요

- 스키마는 **`ai`로 고정** (`public` 아님)
- 테이블: `ai.vector_store` (Spring AI PgVectorStore 표준 컬럼: id/content/metadata/embedding)
- 인덱스: HNSW, cosine distance, `m=16, ef_construction=64`
- `metadata` JSONB에 `source_type`, `source_id`, `job_category_id` 등 검색 필터용 키를 넣어 사용
- 스키마·테이블은 `database/postgresql/ddl` DDL로 직접 관리하며, **`PgVectorConfig.java`에서 `initializeSchema(false)`로 자동 생성을 막고 있음** — 새 환경에 배포할 때는 반드시 DDL을 먼저 실행해야 함
- `application.properties`의 `spring.ai.vectorstore.pgvector.*` 값은 **미사용**(수동 Config가 하드코딩으로 관리) — 값 변경 시 반드시 `PgVectorConfig.java`도 같이 확인

## 5. AI 기능 (3가지 확정 범위)

1. **자기소개서 AI 첨삭** (REQ-901~903) — 첨삭 버튼 클릭 시 대상 채용공고를 선택 → 해당 공고를 pgvector에서 유사도 검색(RAG) → 자소서와 대조해 맞춤 첨삭 생성
2. **채용공고 문장 다듬기** (REQ-506) — 기업회원이 작성한 공고 문장을 OpenAI(`gpt-5-mini`)로 교정
3. **희망직무 기반 맞춤 추천** (REQ-104) — 개인회원의 희망직무 기반으로 관련 채용공고 추천

선행 작업: **REQ-507(채용공고 등록·수정 시 텍스트 임베딩 → pgvector 저장)** — 1·2·3 모두의 전제 조건.

## 6. 현재 구현 상태 (수시 업데이트 필요)

- ✅ 회원가입, 로그인 화면, 기업승인, 자기소개서 CRUD 완료
- ✅ `모집예정` 상태 구현 완료 — V6 마이그레이션(`V6__add_scheduled_job_posting_status.sql`)으로 DB CHECK 제약에 추가됨. `CorpJobService`·`CorpJobQueryService`·`CorpJobManagementService`·`AdminJobService`·`AdminJobModerationService` 전반에 상태 분기 반영됨
- ✅ 채용공고 발행·마감 자동화: `JobPostingScheduleScheduler`(매분 실행, 기존 `JobPostingClosingScheduler`에서 이름 변경)
  - `JobPostingPublishingService.publishScheduledPostings()` — `모집예정` + 시작일 도래 시 자동 `모집중` 전환
  - `JobPostingClosingService.closeExpiredPostings()` — `모집중` + 마감일 도래 시 자동 `마감` 전환
- ⚠️ 입사지원(JobApplicationService), 자소서 AI 첨삭 실제 연동, 지원자관리 서비스 로직: 부분/미구현 — 진행 중
- 기준 문서: **요구사항정의서 / WBS(2026-08-05 갱신본)**가 최신. 노션의 "역할분배"·"사이트구조요약" 문서는 초기 설계 스냅샷이었으나, 게시중→모집중/정지→이용정지 표기 및 AI 첨삭 흐름(공고선택→RAG첨삭)은 2026-08-06 기준 최신화 반영함. 상충 시 요구사항정의서·WBS 우선.

## 7. 참고 링크

- Figma: https://www.figma.com/design/yfEUfs6dqpNpTQOX1sdYrt/
- Notion(요구사항정의서/WBS 상위): https://app.notion.com/p/3a3aa54ab0678013bd6aedcb401f66c1
- 라이브 사이트(실제 최신 화면 기준): http://54.116.131.165:8080/
- GitHub: https://github.com/Jyung01/First-day-project

## 8. 협업 규칙

- **PR 생성·merge는 항상 사용자에게 먼저 동의를 구할 것.** 허락 없이 임의로 push/merge하지 않는다.
- DB 마이그레이션 파일(버전) 추가·수정 시에도 항상 먼저 확인받고 진행한다.
- 최신 DB DDL은 `database/` 폴더의 MySQL/PostgreSQL DDL 파일 기준.
- `main` 브랜치 직접 push 금지, 기능 단위 브랜치 → PR → 승인 후 병합.
- **커밋 메시지에 `Co-Authored-By:` 트레일러를 넣지 않는다.** GitHub이 공동 작성자 아바타를 표시해 커밋 목록이 지저분해진다. 도구 기본값으로 들어가더라도 제거할 것.
- **작업 시작 전 원격 상태를 먼저 확인한다** (`git fetch` 후 `git log origin/main`). 브랜치가 원격에 안 보인다고 "push 안 됨"으로 단정하지 말 것 — merge 후 자동 삭제된 것일 수 있다. `main`의 커밋 해시가 로컬 기준점과 같은지까지 대조한다.
- **코드를 수정한 뒤에는 항상 어떤 파일을 새로 만들었는지·수정했는지, 그리고 작성한 코드 로직이 어떻게 동작하는지 설명할 것.**
