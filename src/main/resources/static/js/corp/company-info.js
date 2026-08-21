document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-company-info-form]");

    if (!form) {
        return;
    }

    const logoInput = form.querySelector("[data-company-logo-input]");
    const logoPreview = form.querySelector("[data-company-logo-preview]");
    const logoMessage = form.querySelector("[data-company-logo-message]");

    const formMessage = form.querySelector("[data-company-form-message]");
    const savedMarker = document.querySelector("[data-company-info-saved]");

    if (savedMarker && typeof showConfirmModal === "function") {
        showConfirmModal({
            iconClass: "success",
            iconHtml: "✓",
            title: "기업정보가 저장되었습니다",
            message: "변경한 기업정보가 정상적으로 반영되었습니다.",
            leftVisible: false,
            rightText: "확인",
            rightClass: "btn-primary"
        });

        const queryParameters = new URLSearchParams(window.location.search);
        queryParameters.delete("saved");
        const queryString = queryParameters.toString();
        history.replaceState(
            null,
            "",
            window.location.pathname + (queryString ? "?" + queryString : "")
        );
    }

    /* =====================================================
       메시지
    ===================================================== */

    function showMessage(element, message, success = false) {
        if (!element) {
            return;
        }

        element.textContent = message;
        element.classList.add("is-visible");
        element.classList.toggle("is-success", success);
    }

    function clearMessage(element) {
        if (!element) {
            return;
        }

        element.textContent = "";
        element.classList.remove("is-visible", "is-success");
    }

    /* =====================================================
       기업 로고 미리보기
    ===================================================== */

    logoInput?.addEventListener("change", function () {
        const file = logoInput.files?.[0];

        clearMessage(logoMessage);

        if (!file) {
            return;
        }

        const allowedTypes = ["image/jpeg", "image/png"];

        if (!allowedTypes.includes(file.type)) {
            logoInput.value = "";

            showMessage(logoMessage, "JPG 또는 PNG 이미지만 등록할 수 있습니다.");

            return;
        }

        const maximumSize = 5 * 1024 * 1024;

        if (file.size > maximumSize) {
            logoInput.value = "";

            showMessage(logoMessage, "기업 로고는 5MB 이하로 등록해주세요.");

            return;
        }

        const reader = new FileReader();

        reader.addEventListener("load", function () {
            if (!logoPreview) {
                return;
            }

            logoPreview.innerHTML = "";

            const image = document.createElement("img");

            image.src = reader.result;
            image.alt = "변경할 기업 로고 미리보기";
            image.dataset.companyLogoImage = "";

            logoPreview.appendChild(image);

            showMessage(logoMessage, "선택한 로고가 미리보기에 반영되었습니다.", true);
        });

        reader.readAsDataURL(file);
    });

    /* =====================================================
       글자 수
    ===================================================== */

    document.querySelectorAll("[data-company-count]").forEach(function (counter) {
        const targetId = counter.dataset.companyCount;
        const target = document.getElementById(targetId);

        if (!target) {
            return;
        }

        function updateCount() {
            counter.textContent = String(target.value.length);
        }

        target.addEventListener("input", updateCount);
        updateCount();
    });

    /* =====================================================
       저장
    ===================================================== */

    form.addEventListener("submit", function (event) {
        clearMessage(formMessage);

        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
            return;
        }

        // 유효한 폼은 서버로 제출하고 저장 완료 후 모달을 표시한다.
    });

    /* =====================================================
       심사 요청 (가입 후 기업정보를 작성 중인 기업에게만 보인다)
    ===================================================== */

    const reviewRequestButton = document.querySelector(
        "[data-company-review-request]"
    );
    const reviewRequestFlag = form.querySelector("[data-review-request-flag]");

    /*
     * 심사 요청은 이 폼을 그대로 제출한다. 화면에 보이는 내용이 곧 심사에 올라가는 내용이 되도록
     * 저장과 심사 요청을 한 번에 보내기 때문이다.
     *
     * 요청하는 순간 심사 큐에 올라가고 로그아웃되며 결과가 나올 때까지 수정할 수 없다.
     * 되돌릴 수 없는 동작이라 확인을 한 번 더 받는다.
     */
    reviewRequestButton?.addEventListener("click", function (event) {
        event.preventDefault();

        clearMessage(formMessage);

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        showConfirmModal({
            iconClass: "warning",
            iconHtml: "?",
            title: "입력한 내용을 저장하고 심사를 요청할까요?",
            message:
                "지금 화면의 기업정보가 저장된 뒤 관리자에게 제출됩니다.\n제출 후에는 심사 결과가 나올 때까지 기업정보를 수정할 수 없고, 기업회원 로그인이 제한됩니다.",
            leftText: "취소",
            rightText: "저장하고 심사 요청",
            leftClass: "btn-outline",
            rightClass: "btn-primary",

            onRight: function () {
                if (reviewRequestFlag) {
                    reviewRequestFlag.value = "true";
                }
                // form.submit()은 submit 이벤트를 발생시키지 않으므로 위에서 직접 검증했다.
                form.submit();
            },
        });
    });
});
