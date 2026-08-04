# MySQL 변경 이력

기존 DB를 다음 버전으로 변경하기 위한 SQL을 순서대로 보관한다.

## 버전

| 버전 | 파일 | 내용 |
|---|---|---|
| V1 | `V1__initial_schema.sql` | 최초 MySQL 스키마 |
| V2 | `V2__change_skills_to_hierarchy.sql` | `skills`에서 `skill_group`을 제거하고 `parent_id`, `depth` 기반 1·2차 계층 구조 적용 |
| V3 | `V3__change_skills_unique_key.sql` | 기술명 고유 기준을 전체 기술명에서 `(parent_id, skill_name)` 조합으로 변경 |

## 규칙

- 이미 팀 공용 DB에 적용한 migration 파일은 수정하지 않는다.
- 추가 수정은 V3, V4처럼 새 파일로 작성한다.
- 적용 전 백업하고, 실행 후 `SHOW COLUMNS`, 인덱스, 외래키 및 CHECK 제약을 확인한다.
- V2는 기존 `skills` 행을 모두 `depth=1`로 먼저 보정한 뒤 NOT NULL 제약을 적용한다.


## V3 주의사항

- `UNIQUE (parent_id, skill_name)`은 동일 부모 아래의 2차 기술명 중복을 차단한다.
- MySQL에서는 `NULL`이 포함된 UNIQUE 조합을 여러 건 허용하므로 `parent_id IS NULL`인 1차 기술명 중복은 이 제약만으로 차단되지 않는다.
- 1차 기술명도 DB에서 반드시 유일해야 한다면 생성 컬럼 또는 별도 검증 방식이 추가로 필요하다.
