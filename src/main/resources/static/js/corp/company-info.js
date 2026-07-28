document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-company-info-form]");

    if (!form) {
        return;
    }

    const logoInput = form.querySelector("[data-company-logo-input]");
    const logoPreview = form.querySelector("[data-company-logo-preview]");
    const logoMessage = form.querySelector("[data-company-logo-message]");

    const phoneInput = form.querySelector("[data-company-phone]");

    const addressSearchButton = form.querySelector("[data-company-address-search]");

    const formMessage = form.querySelector("[data-company-form-message]");

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
       대표 연락처 자동 하이픈
    ===================================================== */

    phoneInput?.addEventListener("input", function () {
        const number = phoneInput.value.replace(/[^0-9]/g, "").slice(0, 11);

        if (number.startsWith("02")) {
            if (number.length <= 2) {
                phoneInput.value = number;
                return;
            }

            if (number.length <= 6) {
                phoneInput.value = number.slice(0, 2) + "-" + number.slice(2);

                return;
            }

            if (number.length <= 10) {
                phoneInput.value =
                    number.slice(0, 2) + "-" + number.slice(2, number.length - 4) + "-" + number.slice(-4);

                return;
            }
        }

        if (number.length <= 3) {
            phoneInput.value = number;
            return;
        }

        if (number.length <= 7) {
            phoneInput.value = number.slice(0, 3) + "-" + number.slice(3);

            return;
        }

        phoneInput.value = number.slice(0, 3) + "-" + number.slice(3, 7) + "-" + number.slice(7);
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
       주소 검색
    ===================================================== */

    addressSearchButton?.addEventListener("click", function () {
        /*
         * Kakao Postcode 스크립트 연결 후 사용
         *
         * new daum.Postcode({
         *     oncomplete: function (data) {
         *         document.getElementById("postcode").value =
         *             data.zonecode;
         *
         *         document.getElementById("address").value =
         *             data.roadAddress || data.jibunAddress;
         *
         *         document.getElementById("addressDetail").focus();
         *     }
         * }).open();
         */

        console.log("Kakao Postcode 연결 예정");
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

        /*
         * 현재 CorpController에는 GET 화면 매핑만 있으므로
         * 화면 테스트 단계에서는 실제 제출을 막는다.
         *
         * POST /corp/company-info 구현 후
         * 아래 event.preventDefault()와 테스트 모달을 제거한다.
         */
        event.preventDefault();

        if (typeof showConfirmModal === "function") {
            showConfirmModal({
                iconClass: "success",
                iconHtml: "✓",
                title: "기업정보가 저장되었습니다",
                message: "변경한 기업정보가 정상적으로 반영되었습니다.",
                leftVisible: false,
                rightText: "확인",
                rightClass: "btn-primary",
            });
        }
    });
});
