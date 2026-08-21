-- 첫출근 통합 DB 설계 v1 - MySQL 업무 데이터 원장
-- Target: MySQL 8.0.16+
-- Baseline: GitHub main f9564b24 (2026-07-29), Figma 최신 화면, Notion·대화 확정 정책
-- 원칙: 확정된 서비스 데이터와 자기소개서 AI 첨삭 이력은 MySQL에 저장한다.
--       임베딩 검색 데이터는 PostgreSQL(pgvector)에 분리하며 DB 간 FK는 만들지 않는다.

SET NAMES utf8mb4;
SET time_zone = '+09:00';
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS firstday
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE firstday;

-- =========================================================
-- 1. 회원 / 인증 / 약관
-- =========================================================

CREATE TABLE users (
  user_id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NULL COMMENT '기업회원이 소속된 기업; 개인·관리자는 NULL',
  login_id              VARCHAR(50) NOT NULL,
  password_hash         VARCHAR(255) NOT NULL,
  name                  VARCHAR(100) NOT NULL COMMENT '개인회원 이름 또는 기업 담당자 이름',
  email                 VARCHAR(255) NOT NULL COMMENT '로그인 계정 이메일; 기업회원은 담당자 이메일',
  phone                 VARCHAR(30) NULL COMMENT '계정 연락처; 기업회원은 담당자 연락처',
  department            VARCHAR(100) NULL COMMENT '기업 담당자 부서; 개인·관리자는 NULL',
  position_title        VARCHAR(100) NULL COMMENT '기업 담당자 직급·직책; 개인·관리자는 NULL',
  user_type             VARCHAR(10) NOT NULL COMMENT '개인/기업/관리자',
  account_status        VARCHAR(10) NOT NULL DEFAULT '정상' COMMENT '정상/이용정지/탈퇴',
  last_login_at         DATETIME(6) NULL,
  password_changed_at   DATETIME(6) NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  withdrawn_at          DATETIME(6) NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_company (company_id),
  UNIQUE KEY uk_users_login_id (login_id),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_type_status (user_type, account_status),
  KEY idx_users_created_at (created_at),
  CONSTRAINT chk_users_type
    CHECK (user_type IN ('개인','기업','관리자')),
  CONSTRAINT chk_users_status
    CHECK (account_status IN ('정상','이용정지','탈퇴'))
) ENGINE=InnoDB COMMENT='개인·기업·관리자 공통 로그인 계정';

CREATE TABLE personal_profiles (
  user_id               BIGINT UNSIGNED NOT NULL,
  postal_code           VARCHAR(20) NULL,
  address_line1         VARCHAR(255) NULL,
  address_line2         VARCHAR(255) NULL,
  profile_image_url     VARCHAR(1000) NULL,
  row_version           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id),
  CONSTRAINT fk_personal_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='개인회원 프로필';

CREATE TABLE policies (
  policy_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  policy_code           VARCHAR(50) NOT NULL,
  title                 VARCHAR(200) NOT NULL,
  audience              VARCHAR(10) NOT NULL DEFAULT '전체' COMMENT '전체/개인/기업',
  consent_type          VARCHAR(10) NOT NULL COMMENT '필수/선택/공개',
  content               LONGTEXT NOT NULL,
  effective_from        DATETIME(6) NULL,
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by            BIGINT UNSIGNED NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_policies_code (policy_code),
  KEY idx_policies_active_order (is_active, display_order),
  CONSTRAINT chk_policies_audience
    CHECK (audience IN ('전체','개인','기업')),
  CONSTRAINT chk_policies_consent_type
    CHECK (consent_type IN ('필수','선택','공개')),
  CONSTRAINT fk_policies_updater
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='회원가입 동의 및 공개 정책; 별도 버전 이력은 관리하지 않음';

CREATE TABLE user_policy_consents (
  consent_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id               BIGINT UNSIGNED NOT NULL,
  policy_id             BIGINT UNSIGNED NOT NULL,
  consented             BOOLEAN NOT NULL,
  consented_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  withdrawn_at          DATETIME(6) NULL,
  ip_address            VARCHAR(45) NULL,
  user_agent            VARCHAR(500) NULL,
  PRIMARY KEY (consent_id),
  UNIQUE KEY uk_user_policy_consent (user_id, policy_id),
  KEY idx_consents_policy (policy_id, consented_at),
  CONSTRAINT fk_consents_user
    FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_consents_policy
    FOREIGN KEY (policy_id) REFERENCES policies(policy_id)
) ENGINE=InnoDB COMMENT='회원별 정책 동의 기록; 약관 버전 이력은 관리하지 않음';

-- =========================================================
-- 2. 기업 / 기업회원
-- =========================================================

CREATE TABLE companies (
  company_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  business_number       VARCHAR(20) NOT NULL,
  company_name          VARCHAR(200) NOT NULL,
  representative_name  VARCHAR(100) NULL,
  established_date      DATE NULL,
  industry_name         VARCHAR(100) NULL,
  company_size          VARCHAR(50) NULL,
  homepage_url          VARCHAR(1000) NULL,
  logo_url              VARCHAR(1000) NULL,
  postal_code           VARCHAR(20) NULL,
  address_line1         VARCHAR(255) NULL,
  address_line2         VARCHAR(255) NULL,
  short_description     VARCHAR(300) NULL,
  introduction          LONGTEXT NULL,
  benefits              LONGTEXT NULL,
  approval_status       VARCHAR(10) NOT NULL DEFAULT '승인대기'
                         COMMENT '가입 심사: 승인대기/승인/반려',
  company_status        VARCHAR(10) NOT NULL DEFAULT '정상'
                         COMMENT '운영 상태: 정상/이용정지/탈퇴',
  latest_rejection_code  VARCHAR(50) NULL
                         COMMENT '최근 가입 반려 사유 코드: MISSING_INFORMATION/FORMAT_ERROR/INAPPROPRIATE_INFORMATION',
  latest_rejection_reason VARCHAR(1000) NULL,
  reviewed_by           BIGINT UNSIGNED NULL,
  reviewed_at           DATETIME(6) NULL,
  reapply_requested_at  DATETIME(6) NULL,
  review_requested_at   DATETIME(6) NULL
                         COMMENT '가장 최근 심사 요청 시각; NULL이면 기업정보 작성 중이라 심사 큐에 노출하지 않음',
  row_version           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  withdrawn_at          DATETIME(6) NULL,
  PRIMARY KEY (company_id),
  UNIQUE KEY uk_companies_business_number (business_number),
  KEY idx_companies_name (company_name),
  KEY idx_companies_approval (approval_status, company_status, created_at),
  KEY idx_companies_review_queue (approval_status, company_status, review_requested_at),
  CONSTRAINT chk_companies_approval_status
    CHECK (approval_status IN ('승인대기','승인','반려')),
  CONSTRAINT chk_companies_company_status
    CHECK (company_status IN ('정상','이용정지','탈퇴')),
  CONSTRAINT chk_companies_rejection_code
    CHECK (
      latest_rejection_code IS NULL
      OR latest_rejection_code IN (
        'MISSING_INFORMATION',
        'FORMAT_ERROR',
        'INAPPROPRIATE_INFORMATION'
      )
    ),
  CONSTRAINT fk_companies_reviewer
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='기업 공개정보; 가입 승인 상태와 이용정지·탈퇴 운영 상태를 분리';

-- =========================================================
-- 3. 직무 / 기술 / 채용공고
-- =========================================================

CREATE TABLE job_categories (
  job_category_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id             BIGINT UNSIGNED NULL,
  category_name         VARCHAR(100) NOT NULL,
  slug                  VARCHAR(120) NOT NULL,
  depth                 TINYINT UNSIGNED NOT NULL DEFAULT 1,
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (job_category_id),
  UNIQUE KEY uk_job_categories_slug (slug),
  UNIQUE KEY uk_job_categories_parent_name (parent_id, category_name),
  KEY idx_job_categories_parent_order (parent_id, is_active, display_order),
  CONSTRAINT chk_job_categories_depth
    CHECK (depth IN (1, 2)),
  CONSTRAINT fk_job_categories_parent
    FOREIGN KEY (parent_id) REFERENCES job_categories(job_category_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='관리자 드래그 정렬 대상 계층형 직무 카테고리';

CREATE TABLE skills (
  skill_id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id             BIGINT UNSIGNED NULL,
  depth                 TINYINT UNSIGNED NOT NULL,
  skill_name            VARCHAR(100) NOT NULL,
  slug                  VARCHAR(120) NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (skill_id),
  UNIQUE KEY uk_skills_parent_name (parent_id, skill_name),
  UNIQUE KEY uk_skills_slug (slug),
  KEY idx_skills_parent_order (parent_id, is_active, display_order),
  CONSTRAINT fk_skills_parent
    FOREIGN KEY (parent_id) REFERENCES skills(skill_id)
    ON DELETE RESTRICT,
  CONSTRAINT chk_skills_depth
    CHECK (depth IN (1, 2)),
  CONSTRAINT chk_skills_parent_depth
    CHECK (
      (depth = 1 AND parent_id IS NULL)
      OR
      (depth = 2 AND parent_id IS NOT NULL)
    )
) ENGINE=InnoDB COMMENT='공고와 이력서가 공통 참조하는 1·2차 계층형 기술 스택 마스터';

CREATE TABLE user_desired_jobs (
  user_id               BIGINT UNSIGNED NOT NULL,
  job_category_id       BIGINT UNSIGNED NULL,
  display_order         TINYINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, job_category_id),
  UNIQUE KEY uk_desired_jobs_order (user_id, display_order),
  KEY idx_desired_jobs_category (job_category_id, user_id),
  CONSTRAINT chk_desired_jobs_order
    CHECK (display_order BETWEEN 0 AND 2),
  CONSTRAINT fk_desired_jobs_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_desired_jobs_category
    FOREIGN KEY (job_category_id) REFERENCES job_categories(job_category_id)
) ENGINE=InnoDB COMMENT='개인회원 희망 직무; 최대 3개는 서비스 계층에서 트랜잭션 검증';

CREATE TABLE job_postings (
  job_posting_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NOT NULL,
  job_category_id       BIGINT UNSIGNED NULL,
  title                 VARCHAR(255) NOT NULL,
  employment_type       VARCHAR(20) COLLATE utf8mb4_unicode_ci NULL,
  career_type           VARCHAR(20) COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  min_experience_years  TINYINT UNSIGNED NULL,
  max_experience_years  TINYINT UNSIGNED NULL,
  education_level       VARCHAR(30) COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  work_region           VARCHAR(100) COLLATE utf8mb4_unicode_ci NULL,
  work_address          VARCHAR(500) NULL,
  salary_text           VARCHAR(100) NULL,
  salary_min            INT UNSIGNED NULL COMMENT '만원 단위',
  salary_max            INT UNSIGNED NULL COMMENT '만원 단위',
  headcount             SMALLINT UNSIGNED NULL DEFAULT NULL,
  apply_start_at        DATETIME(6) NULL,
  apply_end_at          DATETIME(6) NULL,
  introduction          LONGTEXT NULL,
  main_tasks            LONGTEXT COLLATE utf8mb4_unicode_ci NULL,
  qualifications        LONGTEXT COLLATE utf8mb4_unicode_ci NULL,
  preferred_conditions LONGTEXT NULL,
  benefits_json         JSON NULL,
  process_text          TEXT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '임시저장'
                         COMMENT '임시저장/모집예정/모집중/마감/숨김/재검토요청/삭제',
  close_reason          VARCHAR(20) NULL
                         COMMENT '기업마감/마감일도래/기업탈퇴',
  hidden_reason         VARCHAR(1000) NULL COMMENT '관리자가 공고를 숨긴 구체적인 사유',
  hidden_by             BIGINT UNSIGNED NULL COMMENT '공고 숨김을 처리한 관리자 회원 ID',
  hidden_at             DATETIME(6) NULL COMMENT '공고를 숨김 상태로 변경한 시각',
  view_count            BIGINT UNSIGNED NOT NULL DEFAULT 0,
  published_at          DATETIME(6) NULL,
  closed_at             DATETIME(6) NULL,
  row_version           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (job_posting_id),
  KEY idx_job_postings_public
    (status, apply_end_at, published_at),
  KEY idx_job_postings_company (company_id, status, created_at),
  KEY idx_job_postings_category_region
    (job_category_id, work_region, status),
  FULLTEXT KEY ftx_job_postings_search
    (title, introduction, main_tasks, qualifications, preferred_conditions),
  CONSTRAINT chk_job_posting_employment_type
    CHECK (employment_type IN ('정규직','계약직','인턴','프리랜서','파견직','기타')),
  CONSTRAINT chk_job_posting_career_type
    CHECK (career_type IN ('신입','경력','경력무관')),
  CONSTRAINT chk_job_posting_education_level
    CHECK (education_level IN ('학력무관','고졸이상','전문대졸이상','대졸이상','석사이상','박사')),
  CONSTRAINT chk_job_posting_status
    CHECK (status IN ('임시저장','모집예정','모집중','마감','숨김','재검토요청','삭제')),
  CONSTRAINT chk_job_posting_close_reason
    CHECK (close_reason IS NULL OR close_reason IN
      ('기업마감','마감일도래','기업탈퇴')),
  CONSTRAINT chk_job_posting_period
    CHECK (apply_start_at IS NULL OR apply_end_at IS NULL
      OR apply_end_at > apply_start_at),
  CONSTRAINT chk_job_posting_salary
    CHECK (salary_min IS NULL OR salary_max IS NULL
      OR salary_max >= salary_min),
  CONSTRAINT fk_job_postings_company
    FOREIGN KEY (company_id) REFERENCES companies(company_id),
  CONSTRAINT fk_job_postings_category
    FOREIGN KEY (job_category_id) REFERENCES job_categories(job_category_id),
  CONSTRAINT fk_job_postings_hidden_by
    FOREIGN KEY (hidden_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='채용공고 원장; 공고 승인 상태와 승인 이력은 사용하지 않음';

CREATE TABLE job_posting_skills (
  job_posting_id        BIGINT UNSIGNED NOT NULL,
  skill_id              BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (job_posting_id, skill_id),
  KEY idx_job_posting_skills_skill (skill_id),
  CONSTRAINT fk_job_posting_skills_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(job_posting_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_job_posting_skills_skill
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id)
) ENGINE=InnoDB COMMENT='공고별 기술 스택; 등록·수정 최대 5개는 서비스 계층에서 검증';

CREATE TABLE saved_jobs (
  user_id               BIGINT UNSIGNED NOT NULL,
  job_posting_id        BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, job_posting_id),
  KEY idx_saved_jobs_posting (job_posting_id, created_at),
  CONSTRAINT fk_saved_jobs_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_saved_jobs_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(job_posting_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='개인회원 관심 공고';

CREATE TABLE saved_companies (
  user_id               BIGINT UNSIGNED NOT NULL,
  company_id            BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, company_id),
  KEY idx_saved_companies_company (company_id, created_at),
  CONSTRAINT fk_saved_companies_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_saved_companies_company
    FOREIGN KEY (company_id) REFERENCES companies(company_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='개인회원 관심 기업';

-- =========================================================
-- 4. 이력서 / 자기소개서 / AI 첨삭 결과
-- =========================================================

CREATE TABLE resumes (
  resume_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id               BIGINT UNSIGNED NOT NULL,
  title                 VARCHAR(200) NOT NULL,
  applicant_name        VARCHAR(100) NOT NULL,
  email                 VARCHAR(255) NOT NULL,
  phone                 VARCHAR(30) NOT NULL,
  career_type           VARCHAR(20) NOT NULL DEFAULT '신입',
  summary               TEXT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '사용중',
  row_version           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at            DATETIME(6) NULL,
  PRIMARY KEY (resume_id),
  KEY idx_resumes_user (user_id, status, updated_at),
  CONSTRAINT chk_resumes_career_type
    CHECK (career_type IN ('신입','경력')),
  CONSTRAINT chk_resumes_status
    CHECK (status IN ('임시저장','사용중','보관')),
  CONSTRAINT fk_resumes_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='지원용 이력서; 파일 업로드가 아닌 구조화 데이터';

CREATE TABLE resume_educations (
  education_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id             BIGINT UNSIGNED NOT NULL,
  school_name           VARCHAR(200) NOT NULL,
  major                 VARCHAR(200) NULL,
  degree                VARCHAR(100) NULL,
  admission_date        DATE NULL,
  graduation_date       DATE NULL,
  graduation_status     VARCHAR(20) NULL,
  gpa_score             DECIMAL(3,2) NULL,
  gpa_scale             DECIMAL(3,2) NULL,
  display_order         INT NOT NULL DEFAULT 0,
  PRIMARY KEY (education_id),
  KEY idx_resume_educations_resume (resume_id, display_order),
  CONSTRAINT chk_resume_graduation_status
    CHECK (graduation_status IS NULL OR graduation_status IN
      ('재학','휴학','졸업예정','졸업','수료','중퇴')),
  CONSTRAINT chk_resume_gpa
    CHECK (gpa_score IS NULL OR gpa_scale IS NULL
      OR (gpa_score >= 0 AND gpa_score <= gpa_scale)),
  CONSTRAINT fk_resume_educations_resume
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='이력서 학력';

CREATE TABLE resume_careers (
  career_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id             BIGINT UNSIGNED NOT NULL,
  company_name          VARCHAR(200) NOT NULL,
  department            VARCHAR(100) NULL,
  position_title        VARCHAR(100) NULL,
  employment_type       VARCHAR(50) NULL,
  start_date            DATE NOT NULL,
  end_date              DATE NULL,
  is_current            BOOLEAN NOT NULL DEFAULT FALSE,
  description           LONGTEXT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  PRIMARY KEY (career_id),
  KEY idx_resume_careers_resume (resume_id, display_order),
  CONSTRAINT chk_resume_career_employment_type
    CHECK (employment_type IS NULL OR employment_type IN
      ('정규직','계약직','인턴','프리랜서','파견직','기타')),
  CONSTRAINT chk_resume_career_period
    CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT fk_resume_careers_resume
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='이력서 경력';

CREATE TABLE resume_projects (
  project_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  resume_id             BIGINT UNSIGNED NOT NULL,
  project_name          VARCHAR(200) NOT NULL,
  role_text             VARCHAR(300) NULL,
  description           LONGTEXT NULL,
  start_date            DATE NULL,
  end_date              DATE NULL,
  project_url           VARCHAR(1000) NULL,
  display_order         INT NOT NULL DEFAULT 0,
  PRIMARY KEY (project_id),
  KEY idx_resume_projects_resume (resume_id, display_order),
  CONSTRAINT chk_resume_project_period
    CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),
  CONSTRAINT fk_resume_projects_resume
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='이력서 프로젝트';

CREATE TABLE resume_skills (
  resume_id             BIGINT UNSIGNED NOT NULL,
  skill_id              BIGINT UNSIGNED NOT NULL,
  display_order         TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (resume_id, skill_id),
  KEY idx_resume_skills_skill (skill_id),
  CONSTRAINT fk_resume_skills_resume
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_resume_skills_skill
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id)
) ENGINE=InnoDB COMMENT='이력서 보유 기술; 최대 10개는 서비스 계층에서 검증';

CREATE TABLE cover_letters (
  cover_letter_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id               BIGINT UNSIGNED NOT NULL,
  title                 VARCHAR(200) NOT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '사용중',
  row_version           BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  deleted_at            DATETIME(6) NULL,
  PRIMARY KEY (cover_letter_id),
  KEY idx_cover_letters_user (user_id, status, updated_at),
  CONSTRAINT chk_cover_letters_status
    CHECK (status IN ('임시저장','사용중','보관')),
  CONSTRAINT fk_cover_letters_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='공고 연결 없이 재사용하는 문항형 자기소개서 원본';

CREATE TABLE cover_letter_items (
  cover_letter_item_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cover_letter_id       BIGINT UNSIGNED NOT NULL,
  question              VARCHAR(1000) NOT NULL,
  answer                LONGTEXT NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (cover_letter_item_id),
  KEY idx_cover_letter_items_order (cover_letter_id, display_order),
  CONSTRAINT fk_cover_letter_items_cover
    FOREIGN KEY (cover_letter_id) REFERENCES cover_letters(cover_letter_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='자기소개서 원본 문항과 현재 답변';

CREATE TABLE cover_letter_ai_reviews (
  cover_letter_ai_review_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cover_letter_id       BIGINT UNSIGNED NOT NULL,
  job_posting_id        BIGINT UNSIGNED NOT NULL COMMENT '첨삭 대상으로 선택한 채용공고',
  original_content      JSON NOT NULL COMMENT '첨삭 요청 당시 문항·답변 전체 스냅샷',
  revised_content       JSON NOT NULL COMMENT 'AI가 제안한 문항별 수정 답변 전체',
  feedback              LONGTEXT NULL COMMENT '전체 첨삭 요약과 개선 이유',
  rag_context           JSON NULL COMMENT '문항별 RAG 검색 근거 문단; 문항 순서와 같은 순서의 배열',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (cover_letter_ai_review_id),
  KEY idx_cover_letter_ai_reviews (cover_letter_id, created_at),
  KEY idx_cover_letter_ai_reviews_job_posting (job_posting_id),
  CONSTRAINT fk_cover_letter_ai_reviews_cover
    FOREIGN KEY (cover_letter_id) REFERENCES cover_letters(cover_letter_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_cover_letter_ai_reviews_job_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(job_posting_id)
) ENGINE=InnoDB COMMENT='자기소개서 AI 첨삭 1회 결과; 원문·수정본·피드백을 한 행에 보존';

-- =========================================================
-- 5. 입사지원 / 전형
-- =========================================================

CREATE TABLE applications (
  application_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_posting_id        BIGINT UNSIGNED NOT NULL,
  applicant_user_id     BIGINT UNSIGNED NOT NULL,
  resume_id             BIGINT UNSIGNED NULL COMMENT '지원에 사용한 이력서 원본; 원본 삭제 후에는 NULL',
  resume_snapshot_json  JSON NOT NULL COMMENT '지원 완료 시점의 이력서·지원자 연락처 전체 스냅샷',
  cover_letter_id       BIGINT UNSIGNED NULL COMMENT '지원에 사용한 자기소개서 원본; 원본 삭제 후에는 NULL',
  cover_letter_snapshot_json JSON NULL COMMENT '지원 완료 시점의 자기소개서 문항·답변 스냅샷; 자소서 미첨부 시 NULL',
  current_status        VARCHAR(20) NOT NULL DEFAULT '지원완료',
  applied_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  cancelled_at          DATETIME(6) NULL COMMENT '지원완료 단계에서 지원자가 취소한 시각',
  termination_reason    VARCHAR(20) NULL COMMENT '채용종료 원인; 기업탈퇴 또는 회원탈퇴',
  terminated_at         DATETIME(6) NULL COMMENT '지원 절차가 채용종료로 끝난 시각',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  active_application_guard TINYINT
                         GENERATED ALWAYS AS (
                           CASE WHEN current_status = '지원취소' THEN NULL ELSE 1 END
                         ) STORED,
  PRIMARY KEY (application_id),
  UNIQUE KEY uk_applications_active (applicant_user_id, job_posting_id, active_application_guard),
  KEY idx_applications_user_status (applicant_user_id, current_status, applied_at),
  KEY idx_applications_posting_status (job_posting_id, current_status, applied_at),
  KEY idx_applications_cover_letter (cover_letter_id),
  CONSTRAINT chk_applications_status
    CHECK (current_status IN
      ('지원완료','서류검토중','서류합격','면접예정','면접완료',
       '최종합격','입사완료','불합격','지원취소','채용종료')),
  CONSTRAINT chk_applications_termination
    CHECK (termination_reason IS NULL OR termination_reason IN ('기업탈퇴','회원탈퇴')),
  CONSTRAINT fk_applications_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(job_posting_id),
  CONSTRAINT fk_applications_applicant
    FOREIGN KEY (applicant_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_applications_resume
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_applications_cover_letter
    FOREIGN KEY (cover_letter_id) REFERENCES cover_letters(cover_letter_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='공고당 1회 지원과 제출 당시 이력서·자기소개서 JSON; 지원취소 후에는 같은 공고 재지원 가능';

CREATE TABLE application_status_history (
  application_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  application_id        BIGINT UNSIGNED NOT NULL,
  from_status           VARCHAR(20) NULL,
  to_status             VARCHAR(20) NOT NULL,
  change_reason         VARCHAR(1000) NULL,
  changed_by            BIGINT UNSIGNED NULL,
  actor_type            VARCHAR(10) NOT NULL,
  changed_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (application_status_id),
  KEY idx_application_history (application_id, changed_at),
  CONSTRAINT chk_application_history_actor
    CHECK (actor_type IN ('지원자','기업','관리자','시스템')),
  CONSTRAINT fk_application_history_application
    FOREIGN KEY (application_id) REFERENCES applications(application_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_application_history_actor
    FOREIGN KEY (changed_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='지원완료부터 입사·불합격·채용종료까지 전형 변경 이력';

CREATE TABLE application_memos (
  application_memo_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  application_id        BIGINT UNSIGNED NOT NULL,
  author_user_id        BIGINT UNSIGNED NOT NULL,
  memo                  TEXT NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (application_memo_id),
  KEY idx_application_memos (application_id, updated_at),
  CONSTRAINT fk_application_memos_application
    FOREIGN KEY (application_id) REFERENCES applications(application_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_application_memos_author
    FOREIGN KEY (author_user_id) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='기업 채용담당자 내부 메모';

-- =========================================================
-- 6. 기업리뷰 / 면접후기 / 연봉
-- =========================================================

CREATE TABLE company_reviews (
  company_review_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NOT NULL,
  author_user_id        BIGINT UNSIGNED NOT NULL,
  eligibility_application_id BIGINT UNSIGNED NOT NULL,
  employment_status     VARCHAR(10) NOT NULL,
  job_category_id       BIGINT UNSIGNED NULL,
  career_growth_rating  TINYINT UNSIGNED NOT NULL,
  work_satisfaction_rating TINYINT UNSIGNED NOT NULL,
  compensation_rating   TINYINT UNSIGNED NOT NULL,
  culture_rating        TINYINT UNSIGNED NOT NULL,
  overall_rating        DECIMAL(2,1) NOT NULL,
  pros                  TEXT NOT NULL,
  cons                  TEXT NOT NULL,
  summary               VARCHAR(300) NOT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '정상',
  hidden_reason         VARCHAR(1000) NULL,
  hidden_by             BIGINT UNSIGNED NULL,
  admin_memo            VARCHAR(2000) NULL COMMENT '관리자 후기 처리 메모',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (company_review_id),
  UNIQUE KEY uk_company_review_author_company
    (author_user_id, company_id),
  UNIQUE KEY uk_company_review_application
    (eligibility_application_id),
  KEY idx_company_reviews_public (company_id, status, created_at),
  CONSTRAINT chk_company_review_employment_status
    CHECK (employment_status IN ('현직원','전직원')),
  CONSTRAINT chk_company_review_status
    CHECK (status IN ('정상','숨김','삭제')),
  CONSTRAINT chk_company_review_ratings
    CHECK (
      career_growth_rating BETWEEN 1 AND 5
      AND work_satisfaction_rating BETWEEN 1 AND 5
      AND compensation_rating BETWEEN 1 AND 5
      AND culture_rating BETWEEN 1 AND 5
      AND overall_rating BETWEEN 1.0 AND 5.0
    ),
  CONSTRAINT fk_company_reviews_company
    FOREIGN KEY (company_id) REFERENCES companies(company_id),
  CONSTRAINT fk_company_reviews_author
    FOREIGN KEY (author_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_company_reviews_application
    FOREIGN KEY (eligibility_application_id) REFERENCES applications(application_id),
  CONSTRAINT fk_company_reviews_category
    FOREIGN KEY (job_category_id) REFERENCES job_categories(job_category_id)
    ON DELETE SET NULL,
  CONSTRAINT fk_company_reviews_hidden_by
    FOREIGN KEY (hidden_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='입사완료 이력 기반 익명 기업리뷰; 회원·기업당 1건';

CREATE TABLE interview_reviews (
  interview_review_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NOT NULL,
  author_user_id        BIGINT UNSIGNED NOT NULL,
  job_posting_id        BIGINT UNSIGNED NOT NULL,
  application_id        BIGINT UNSIGNED NOT NULL,
  interview_month       CHAR(7) NOT NULL COMMENT 'YYYY-MM',
  interview_type        VARCHAR(30) NOT NULL,
  interview_result      VARCHAR(10) NOT NULL,
  difficulty            VARCHAR(10) NOT NULL,
  process_text          TEXT NOT NULL,
  content               LONGTEXT NOT NULL,
  tips                  TEXT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '정상',
  hidden_reason         VARCHAR(1000) NULL,
  hidden_by             BIGINT UNSIGNED NULL,
  admin_memo            VARCHAR(2000) NULL COMMENT '관리자 후기 처리 메모',
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (interview_review_id),
  UNIQUE KEY uk_interview_review_application (application_id),
  KEY idx_interview_reviews_public (company_id, status, created_at),
  CONSTRAINT chk_interview_review_type
    CHECK (interview_type IN ('대면면접','화상면접','전화면접','기타')),
  CONSTRAINT chk_interview_review_result
    CHECK (interview_result IN ('합격','불합격','대기','포기')),
  CONSTRAINT chk_interview_review_difficulty
    CHECK (difficulty IN ('쉬움','보통','어려움')),
  CONSTRAINT chk_interview_review_status
    CHECK (status IN ('정상','숨김','삭제')),
  CONSTRAINT fk_interview_reviews_company
    FOREIGN KEY (company_id) REFERENCES companies(company_id),
  CONSTRAINT fk_interview_reviews_author
    FOREIGN KEY (author_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_interview_reviews_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings(job_posting_id),
  CONSTRAINT fk_interview_reviews_application
    FOREIGN KEY (application_id) REFERENCES applications(application_id),
  CONSTRAINT fk_interview_reviews_hidden_by
    FOREIGN KEY (hidden_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='면접완료 이력 기반 익명 면접후기; 지원 건당 1건';

CREATE TABLE review_reactions (
  user_id               BIGINT UNSIGNED NOT NULL,
  review_type           VARCHAR(10) NOT NULL COMMENT '기업리뷰/면접후기',
  review_id             BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, review_type, review_id),
  KEY idx_review_reactions_target (review_type, review_id),
  CONSTRAINT chk_review_reactions_type
    CHECK (review_type IN ('기업리뷰','면접후기')),
  CONSTRAINT fk_review_reactions_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='기업리뷰·면접후기 도움돼요; 다형 대상은 서비스 계층에서 무결성 검증';

CREATE TABLE salary_records (
  salary_record_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  company_id            BIGINT UNSIGNED NOT NULL,
  author_user_id        BIGINT UNSIGNED NOT NULL,
  job_category_id       BIGINT UNSIGNED NOT NULL,
  employment_status     VARCHAR(10) NOT NULL,
  employment_type       VARCHAR(30) NOT NULL,
  career_years          TINYINT UNSIGNED NOT NULL,
  salary_year           SMALLINT UNSIGNED NOT NULL,
  base_salary           INT UNSIGNED NOT NULL COMMENT '세전 연봉, 만원 단위',
  bonus_amount          INT UNSIGNED NULL COMMENT '성과급, 만원 단위',
  consented_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  status                VARCHAR(10) NOT NULL DEFAULT '정상',
  hidden_reason         VARCHAR(1000) NULL,
  hidden_by             BIGINT UNSIGNED NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (salary_record_id),
  UNIQUE KEY uk_salary_record_condition
    (author_user_id, company_id, salary_year),
  KEY idx_salary_company_public (company_id, status, salary_year),
  KEY idx_salary_category_career (job_category_id, career_years, status),
  CONSTRAINT chk_salary_employment_status
    CHECK (employment_status IN ('현직원','전직원')),
  CONSTRAINT chk_salary_employment_type
    CHECK (employment_type IN ('정규직','계약직','인턴','프리랜서','파견직','기타')),
  CONSTRAINT chk_salary_year
    CHECK (salary_year BETWEEN 1950 AND 2200),
  CONSTRAINT chk_salary_status
    CHECK (status IN ('정상','숨김','삭제')),
  CONSTRAINT fk_salary_records_company
    FOREIGN KEY (company_id) REFERENCES companies(company_id),
  CONSTRAINT fk_salary_records_author
    FOREIGN KEY (author_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_salary_records_category
    FOREIGN KEY (job_category_id) REFERENCES job_categories(job_category_id),
  CONSTRAINT fk_salary_records_hidden_by
    FOREIGN KEY (hidden_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='지원 이력 검증 없는 자기신고 연봉; 회원·기업·연도당 1건, 동일 조건 3건 이상 집계 노출';

-- =========================================================
-- 7. 고객센터 / 신고
-- =========================================================

CREATE TABLE notices (
  notice_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title                 VARCHAR(255) NOT NULL,
  content               LONGTEXT NOT NULL,
  is_pinned             BOOLEAN NOT NULL DEFAULT FALSE,
  status                VARCHAR(10) NOT NULL DEFAULT '임시저장',
  published_at          DATETIME(6) NULL,
  created_by            BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (notice_id),
  KEY idx_notices_public (status, is_pinned, published_at),
  FULLTEXT KEY ftx_notices_search (title, content),
  CONSTRAINT chk_notices_status
    CHECK (status IN ('임시저장','공개','숨김')),
  CONSTRAINT fk_notices_creator
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='고객센터 공지사항';

CREATE TABLE faq_categories (
  faq_category_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_name         VARCHAR(100) NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (faq_category_id),
  UNIQUE KEY uk_faq_categories_name (category_name),
  KEY idx_faq_categories_order (is_active, display_order)
) ENGINE=InnoDB COMMENT='관리 화면 없이 초기 SQL로 고정하는 FAQ 조회·필터 분류';

INSERT INTO faq_categories (category_name, display_order) VALUES
  ('회원·계정', 1),
  ('채용공고', 2),
  ('지원', 3),
  ('이력서', 4),
  ('기업서비스', 5);

CREATE TABLE faqs (
  faq_id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  faq_category_id       BIGINT UNSIGNED NOT NULL,
  question              VARCHAR(500) NOT NULL,
  answer                LONGTEXT NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  status                VARCHAR(10) NOT NULL DEFAULT '공개',
  created_by            BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (faq_id),
  KEY idx_faqs_category_status
    (faq_category_id, status, display_order),
  FULLTEXT KEY ftx_faqs_search (question, answer),
  CONSTRAINT chk_faqs_status
    CHECK (status IN ('공개','숨김')),
  CONSTRAINT fk_faqs_category
    FOREIGN KEY (faq_category_id) REFERENCES faq_categories(faq_category_id),
  CONSTRAINT fk_faqs_creator
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='자주 묻는 질문';

CREATE TABLE inquiry_categories (
  inquiry_category_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_name         VARCHAR(100) NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (inquiry_category_id),
  UNIQUE KEY uk_inquiry_categories_name (category_name),
  KEY idx_inquiry_categories_order (is_active, display_order)
) ENGINE=InnoDB COMMENT='1:1 문의 유형';

CREATE TABLE inquiries (
  inquiry_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id               BIGINT UNSIGNED NOT NULL,
  inquiry_category_id   BIGINT UNSIGNED NOT NULL,
  title                 VARCHAR(100) NOT NULL,
  content               VARCHAR(1000) NOT NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '접수',
  answer_content        LONGTEXT NULL,
  answered_by           BIGINT UNSIGNED NULL,
  answered_at           DATETIME(6) NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (inquiry_id),
  KEY idx_inquiries_user (user_id, status, created_at),
  KEY idx_inquiries_admin_queue
    (status, inquiry_category_id, created_at),
  CONSTRAINT chk_inquiries_status
    CHECK (status IN ('접수','처리중','답변완료')),
  CONSTRAINT fk_inquiries_user
    FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_inquiries_category
    FOREIGN KEY (inquiry_category_id)
    REFERENCES inquiry_categories(inquiry_category_id),
  CONSTRAINT fk_inquiries_answerer
    FOREIGN KEY (answered_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='1:1 문의와 관리자 답변';

CREATE TABLE inquiry_attachments (
  inquiry_attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  inquiry_id            BIGINT UNSIGNED NOT NULL,
  original_name         VARCHAR(255) NOT NULL,
  storage_key           VARCHAR(1000) NOT NULL,
  mime_type             VARCHAR(100) NOT NULL,
  file_size             BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (inquiry_attachment_id),
  KEY idx_inquiry_attachments_inquiry (inquiry_id),
  CONSTRAINT chk_inquiry_attachment_size
    CHECK (file_size <= 10485760),
  CONSTRAINT fk_inquiry_attachments_inquiry
    FOREIGN KEY (inquiry_id) REFERENCES inquiries(inquiry_id)
    ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='1:1 문의 첨부 메타데이터; 파일은 객체 스토리지, 최대 3개는 서비스 검증';

CREATE TABLE reports (
  report_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reporter_user_id      BIGINT UNSIGNED NOT NULL,
  target_type           VARCHAR(20) NOT NULL,
  target_id             BIGINT UNSIGNED NOT NULL,
  reason_code           VARCHAR(30) NOT NULL,
  detail                VARCHAR(2000) NULL,
  status                VARCHAR(10) NOT NULL DEFAULT '미처리',
  resolution_action     VARCHAR(10) NULL,
  resolution_note       VARCHAR(2000) NULL,
  handled_by            BIGINT UNSIGNED NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  handled_at            DATETIME(6) NULL,
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_reports_duplicate
    (reporter_user_id, target_type, target_id),
  KEY idx_reports_queue (status, target_type, created_at),
  CONSTRAINT chk_reports_target
    CHECK (target_type IN ('기업','채용공고','기업리뷰','면접후기')),
  CONSTRAINT chk_reports_reason
    CHECK (reason_code IN
      ('허위 정보·사기 의심','개인정보 노출','욕설·비방·차별 표현',
       '광고·스팸·중복 콘텐츠','기타 운영정책 위반')),
  CONSTRAINT chk_reports_status
    CHECK (status IN ('미처리','처리완료','기각')),
  CONSTRAINT chk_reports_action
    CHECK (resolution_action IS NULL OR resolution_action IN
      ('없음','경고','숨김','삭제','이용정지')),
  CONSTRAINT fk_reports_reporter
    FOREIGN KEY (reporter_user_id) REFERENCES users(user_id),
  CONSTRAINT fk_reports_handler
    FOREIGN KEY (handled_by) REFERENCES users(user_id)
    ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='기업·공고·기업리뷰·면접후기 신고; 다형 대상 무결성은 서비스 계층에서 검증';

-- =========================================================
-- 8. 사이트 운영 / 관리자 감사
-- =========================================================

CREATE TABLE banners (
  banner_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  banner_name           VARCHAR(200) NOT NULL,
  placement             VARCHAR(30) NOT NULL,
  image_url             VARCHAR(1000) NOT NULL,
  link_url              VARCHAR(1000) NULL,
  alt_text              VARCHAR(255) NOT NULL,
  display_order         INT NOT NULL DEFAULT 0,
  starts_at             DATETIME(6) NULL,
  ends_at               DATETIME(6) NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_by            BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (banner_id),
  KEY idx_banners_display
    (placement, is_active, starts_at, ends_at, display_order),
  CONSTRAINT chk_banner_placement
    CHECK (placement IN ('main','job','companies')),
  CONSTRAINT chk_banner_period
    CHECK (starts_at IS NULL OR ends_at IS NULL OR ends_at > starts_at),
  CONSTRAINT fk_banners_creator
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='메인 등 위치별 운영 배너';

CREATE TABLE site_settings (
  setting_key           VARCHAR(100) NOT NULL,
  setting_value         JSON NOT NULL,
  updated_by            BIGINT UNSIGNED NOT NULL,
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (setting_key),
  CONSTRAINT fk_site_settings_updater
    FOREIGN KEY (updated_by) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='서비스명·로고·푸터·고객센터 등 사이트 설정; 비밀값 저장 금지';

CREATE TABLE site_versions (
  site_version_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  version_name          VARCHAR(50) NOT NULL,
  change_notes          LONGTEXT NOT NULL,
  created_by            BIGINT UNSIGNED NOT NULL,
  created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                         ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (site_version_id),
  UNIQUE KEY uk_site_versions_name (version_name),
  KEY idx_site_versions_created (created_at),
  CONSTRAINT fk_site_versions_creator
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB COMMENT='관리자 수동 사이트 버전·변경내역; 삭제하지 않음';


ALTER TABLE users
  ADD CONSTRAINT fk_users_company
  FOREIGN KEY (company_id) REFERENCES companies(company_id);

SET FOREIGN_KEY_CHECKS = 1;
