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
});
