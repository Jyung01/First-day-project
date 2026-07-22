# 첫출근 프로젝트 가이드

이 폴더는 `00_공통 레이아웃 기준`을 확장한 프로젝트 공통 안내 영역이다. Figma 페이지 이름도 `00_프로젝트 가이드`로 통일한다.

## 현재 산출물 범위

- 일반 화면 HTML: 80개
- 공통 조각 HTML: 6개
- `css/`: HTML 영역과 같은 폴더 구조만 생성하며 CSS 파일은 만들지 않는다.
- `js/`: 빈 폴더 하나만 유지한다.
- `images/`: 빈 폴더 하나만 유지한다.
- `modal/`: 빈 폴더 하나만 유지한다. 각 담당자가 필요한 모달을 만들고 JS로 불러 사용한다.
- 화면의 디자인과 더미데이터는 아직 작성하지 않은 빈 HTML 파일 상태다.

## 폴더 역할

| 폴더 | 역할 |
|---|---|
| `auth/` | 로그인, 개인·기업회원 가입 |
| `job/` | 채용공고 조회와 입사지원 |
| `company/` | 기업정보, 기업리뷰, 면접후기 |
| `salary/` | 연봉정보 조회·등록·수정 |
| `my/` | 개인회원 마이페이지, 이력서, 자기소개서, AI 첨삭 결과 |
| `cs/` | 공지사항, FAQ, 1:1 문의 |
| `corp/` | 기업회원 전용 기업관리 |
| `admin/` | 관리자 화면 |
| `policy/` | 이용약관과 개인정보처리방침 |
| `common/` | 사용자·기업·관리자 공통 헤더와 푸터 조각 |
| `modal/` | JS로 불러 사용할 모달 보관 위치 |
| `css/` | 화면 영역별 CSS 폴더 구조 |
| `js/` | 공통 및 페이지 JS를 추후 추가할 위치 |
| `images/` | 이미지 파일을 추후 추가할 위치 |

## 전체 구조 요약

```text
Firstday/
├─ index.html
├─ 00_프로젝트_가이드/
│  ├─ README.md
│  ├─ PAGE_MAP.md
│  └─ MODAL_MAP.md
├─ common/
├─ auth/
├─ job/
├─ company/
├─ salary/
├─ my/
│  ├─ resume/
│  └─ cover-letter/
├─ cs/
│  ├─ notice/
│  ├─ faq/
│  └─ qna/
├─ corp/
├─ admin/
│  ├─ member/
│  ├─ company/
│  ├─ job/
│  ├─ config/
│  ├─ review/
│  ├─ report/
│  ├─ salary/
│  ├─ banner/
│  └─ cs/
├─ policy/
├─ css/        # 화면 영역과 같은 하위 폴더, 파일 없음
├─ js/         # 빈 폴더
├─ images/     # 빈 폴더
└─ modal/      # 빈 폴더
```

## 파일명 규칙

- 영문 소문자와 하이픈을 사용한다: `application-detail.html`
- 목록의 대표 파일은 `index.html` 또는 `list.html`로 구분한다.
- 등록·수정 폼을 같은 화면에서 재사용하면 `form.html`을 사용한다.
- 화면 상태만 다른 경우 화면번호에 맞춘 별도 HTML을 두되, 실제 Spring 구현 단계에서는 한 템플릿으로 합쳐도 된다.
- Figma 화면번호와 파일 경로는 [PAGE_MAP.md](./PAGE_MAP.md)를 기준으로 확인한다.
- 모달 목록은 [MODAL_MAP.md](./MODAL_MAP.md)에서 확인하되 현재는 모달 HTML을 만들지 않는다.

## 공통 레이아웃 적용 기준

- 일반 사용자 화면: `common/header.html`, `common/footer.html`
- 기업관리 화면: `common/corp-header.html`, `common/corp-footer.html`
- 관리자 화면: `common/admin-header.html`, `common/admin-footer.html`
- 현재 파일은 정적 HTML 설계용이다. Spring Boot 적용 시 Thymeleaf Fragment로 전환한다.

## 작업 순서

1. 담당 화면의 Figma 번호를 확인한다.
2. `PAGE_MAP.md`에서 대응 HTML 경로를 찾는다.
3. 해당 HTML에 Figma 디자인과 더미데이터를 구현한다.
4. 화면별 CSS가 필요해지면 대응되는 `css/` 하위 폴더에 파일을 추가한다.
5. 모달이 필요하면 `modal/`에 HTML을 추가하고 `js/`의 스크립트에서 불러온다.
