/**
 * CSRF 토큰을 fetch 요청에 자동으로 실어주는 전역 래퍼.
 *
 * Spring Security의 CookieCsrfTokenRepository가 XSRF-TOKEN 쿠키로 토큰을 내려주면,
 * 이 스크립트가 window.fetch를 감싸 상태변경 요청에 X-XSRF-TOKEN 헤더를 붙인다.
 * 각 화면의 JS는 고칠 필요가 없고, 앞으로 새로 작성하는 fetch도 자동으로 적용된다.
 *
 * 공통 헤더 프래그먼트(common/header, admin-header, corp-header)에서 불러오므로
 * 페이지 하단의 화면별 스크립트보다 항상 먼저 로드된다.
 */
(function () {
    "use strict";

    var COOKIE_NAME = "XSRF-TOKEN";
    var HEADER_NAME = "X-XSRF-TOKEN";

    // Spring Security가 CSRF 검증을 건너뛰는 메서드. 여기에 해당하면 토큰을 붙이지 않는다.
    var SAFE_METHODS = ["GET", "HEAD", "OPTIONS", "TRACE"];

    var originalFetch = window.fetch;
    if (typeof originalFetch !== "function") {
        return;
    }

    function readToken() {
        var cookies = document.cookie ? document.cookie.split(";") : [];
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i].trim();
            if (cookie.indexOf(COOKIE_NAME + "=") === 0) {
                return decodeURIComponent(cookie.substring(COOKIE_NAME.length + 1));
            }
        }
        return null;
    }

    function isSafeMethod(method) {
        return SAFE_METHODS.indexOf(String(method || "GET").toUpperCase()) !== -1;
    }

    /**
     * 토큰이 외부 사이트로 새어나가지 않도록 같은 출처인지 확인한다.
     * "/path" 같은 상대 경로는 URL 생성자가 현재 출처로 해석해 준다.
     */
    function isSameOrigin(input) {
        try {
            return new URL(input, window.location.href).origin === window.location.origin;
        } catch (error) {
            // 해석할 수 없는 입력이면 토큰을 붙이지 않는 쪽이 안전하다.
            return false;
        }
    }

    /**
     * fetch(url, init) 과 fetch(Request) 두 형태를 모두 받는다.
     * Request 객체는 url/method가 자기 안에 들어 있어 init이 비어 있을 수 있다.
     */
    function isRequestObject(input) {
        return typeof Request !== "undefined" && input instanceof Request;
    }

    function resolveTarget(input, init) {
        var isRequest = isRequestObject(input);
        return {
            url: isRequest ? input.url : String(input),
            method: (init && init.method) || (isRequest ? input.method : "GET")
        };
    }

    window.fetch = function (input, init) {
        var target = resolveTarget(input, init);

        if (isSafeMethod(target.method) || !isSameOrigin(target.url)) {
            return originalFetch.apply(this, arguments);
        }

        var token = readToken();
        if (!token) {
            // 아직 토큰이 발급되지 않은 상태(예: CSRF 비활성). 그대로 통과시킨다.
            return originalFetch.apply(this, arguments);
        }

        // 기존 헤더를 덮어쓰지 않고 병합한다.
        // 호출부가 Content-Type이나 X-Requested-With를 이미 지정한 경우가 있다.
        var nextInit = Object.assign({}, init);
        var headers = new Headers(
            (init && init.headers) || (isRequestObject(input) ? input.headers : undefined)
        );
        headers.set(HEADER_NAME, token);
        nextInit.headers = headers;

        return originalFetch.call(this, input, nextInit);
    };
})();
