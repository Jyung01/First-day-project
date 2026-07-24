# Thymeleaf 이전 준비 안내

현재 구조는 정적 HTML 미리보기와 Spring Boot + Thymeleaf 사용을 모두 지원하도록 구성했다.

## 정적 HTML에서

각 페이지는 다음 구조를 사용한다.

```html
<div data-include="/common/header.html"
     th:replace="~{common/header :: header}"></div>
```

- 정적 HTML: `data-include` + `/js/include.js`
- Thymeleaf: `th:replace`

정적 화면은 파일을 직접 더블클릭하지 말고 Live Server 같은 로컬 웹 서버에서 실행한다.

## Spring Boot로 옮길 위치

```text
src/main/resources/
├─ templates/
│  ├─ index.html
│  ├─ common/
│  │  ├─ header.html
│  │  ├─ footer.html
│  │  ├─ corp-header.html
│  │  ├─ corp-footer.html
│  │  ├─ admin-header.html
│  │  └─ admin-footer.html
│  └─ 나머지 화면 폴더
└─ static/
   ├─ css/
   ├─ js/
   └─ images/
```

## Spring 이전 시 할 일

1. 화면 HTML과 `common` 폴더를 `templates`로 이동
2. `css`, `js`, `images`를 `static`으로 이동
3. URL별 Controller 작성
4. `/js/include.js`는 삭제해도 되고 남겨둬도 된다
5. `index.html`의 include 스크립트에는 `th:remove="all"`이 있어 Thymeleaf 처리 시 자동 제거된다

## 공통 fragment

일반 화면:

```html
<div data-include="/common/header.html"
     th:replace="~{common/header :: header}"></div>

<div data-include="/common/footer.html"
     th:replace="~{common/footer :: footer}"></div>
```

기업관리 화면:

```html
<div data-include="/common/corp-header.html"
     th:replace="~{common/corp-header :: corpHeader}"></div>
```

관리자 화면:

```html
<div data-include="/common/admin-header.html"
     th:replace="~{common/admin-header :: adminHeader}"></div>
```

## Controller 예시

```java
@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/job/list")
    public String jobList() {
        return "job/list";
    }
}
```
