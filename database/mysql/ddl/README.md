# MySQL 최신 DDL

`firstday_mysql_current.sql`은 모든 migration이 반영된 현재 최종 전체 스키마다.

- 새 데이터베이스 생성용으로 사용한다.
- DB 구조가 바뀔 때마다 이 파일도 최신 상태로 수정한다.
- 과거 구조 확인은 `../migration`의 버전별 SQL을 사용한다.


현재 파일에는 V1~V3 변경사항이 모두 반영되어 있다.

- V2: `skills` 1·2차 계층 구조 적용
- V3: `uk_skills_name` 제거 및 `uk_skills_parent_name (parent_id, skill_name)` 적용
