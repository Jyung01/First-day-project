# MySQL

첫출근 서비스의 회원, 기업, 공고, 지원, 이력서, 후기, 고객센터 및 운영 데이터를 관리한다.

## 적용 순서

새 DB는 `ddl/firstday_mysql_current.sql`을 실행한다.
기존 V1 DB는 `migration/V2__change_skills_to_hierarchy.sql`을 실행한다.

현재 최신 스키마 버전: **V2**


## 현재 MySQL 스키마 버전

- 최신 변경 버전: V3
- 최신 DDL: `ddl/firstday_mysql_current.sql`
- 마지막 변경: `skills`의 UNIQUE 인덱스를 `uk_skills_parent_name (parent_id, skill_name)`으로 변경
