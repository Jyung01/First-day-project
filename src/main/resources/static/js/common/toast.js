/**
 * 저장·삭제 완료 안내 토스트.
 *
 *   showToast("변경사항이 저장되었습니다.");
 *   showToast("저장하지 못했습니다.", "error");
 *
 * 화면에 common/toast fragment가 있어야 하며, 없으면 조용히 무시한다.
 */
(function () {
    var VISIBLE_MS = 3000;

    function showToast(message, type) {
        if (!message) return;

        var container = document.getElementById("toastContainer");
        if (!container) return;

        var toast = document.createElement("div");
        toast.className = type === "error" ? "toast is-error" : "toast";
        toast.textContent = message;
        container.appendChild(toast);

        // 삽입 직후 클래스를 바꿔야 transition이 걸린다.
        requestAnimationFrame(function () {
            toast.classList.add("is-visible");
        });

        window.setTimeout(function () {
            toast.classList.remove("is-visible");
            // 사라지는 효과가 끝난 뒤 제거한다.
            toast.addEventListener("transitionend", function () {
                toast.remove();
            }, { once: true });
        }, VISIBLE_MS);
    }

    /**
     * 리다이렉트로 전달된 결과 파라미터를 읽어 토스트를 띄우고 URL에서 지운다.
     * 지우지 않으면 새로고침할 때마다 같은 안내가 다시 뜬다.
     *
     * message에는 문자열을 주거나, 파라미터 값에 따라 문구를 바꾸려면
     * { created: "등록되었습니다.", updated: "수정되었습니다." } 형태의 객체를 준다.
     * 객체에 없는 값이 오면 아무것도 띄우지 않는다.
     */
    function showToastFromQuery(paramName, message, type) {
        var params = new URLSearchParams(window.location.search);
        if (!params.has(paramName)) return;

        var text = typeof message === "string"
            ? message
            : message[params.get(paramName)];

        if (text) {
            showToast(text, type);
        }

        params.delete(paramName);
        var query = params.toString();
        window.history.replaceState(
            null,
            "",
            window.location.pathname + (query ? "?" + query : "")
        );
    }

    window.showToast = showToast;
    window.showToastFromQuery = showToastFromQuery;
})();
