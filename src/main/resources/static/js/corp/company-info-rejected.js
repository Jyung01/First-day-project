document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-rejected-company-form]");

    if (!form) {
        return;
    }

    const formMessage = form.querySelector("[data-rejected-form-message]");

    const logoInput = form.querySelector("[data-company-logo-input]");
    const logoPreview = form.querySelector("[data-company-logo-preview]");
    const logoMessage = form.querySelector("[data-company-logo-message]");

    const businessNumberInput = form.querySelector("[data-business-number]");
    const businessNumberError = form.querySelector(
        "[data-business-number-error]"
    );

    const initiallyRejectedFields = Array.from(
        form.querySelectorAll(".company-input.is-rejected, .company-textarea.is-rejected")
    );

    /* 관리자가 지정한 반려 강조는 사용자가 값을 수정하면 해제한다. */
    initiallyRejectedFields.forEach(function (field) {
        const eventName = field.matches("select") ? "change" : "input";

        field.addEventListener(eventName, function () {
            field.classList.remove("is-rejected");
        });
    });

    /**
     * 검증에 걸린 입력에 강조를 붙이고, 값을 고치면 스스로 지워지도록 리스너를 함께 단다.
     *
     * 위 initiallyRejectedFields는 페이지가 그려진 시점에 관리자가 반려한 항목만 담는다.
     * 검증에 걸리는 항목은 거기 없을 수 있고(예: 대표자명), 그러면 강조를 지워 줄 리스너가
     * 없어 값을 채워도 붉은 테두리가 그대로 남는다. 그래서 붙일 때 같이 등록한다.
     */
    function markInvalid(field) {
        if (!field) {
            return;
        }

        field.classList.add("is-rejected");

        const eventName = field.matches("select") ? "change" : "input";

        field.addEventListener(
            eventName,
            function () {
                field.classList.remove("is-rejected");
            },
            { once: true }
        );
    }

    /*
     * 서버 검증에 걸려 화면이 다시 그려진 경우에도 같은 통로로 알린다.
     * 이때 문구는 이미 폼 하단에 그려져 있지만 그대로 두고 토스트만 덧붙인다.
     * 토스트는 3초 뒤 사라지므로, 남아 있는 문구가 나중에 다시 확인할 수단이 된다.
     */
    const serverMessage = formMessage?.textContent.trim();

    if (serverMessage && typeof window.showToast === "function") {
        window.showToast(serverMessage, "error");
    }

    const rejectedBenefitBox = form.querySelector(
        ".company-benefit-selection-box.is-rejected"
    );
    const benefitChipList = form.querySelector("[data-benefit-chip-list]");

    /* 복지를 추가하거나 삭제하면 관리자 반려 강조를 해제한다. */
    if (rejectedBenefitBox && benefitChipList) {
        const benefitChangeObserver = new MutationObserver(function () {
            rejectedBenefitBox.classList.remove("is-rejected");
            benefitChangeObserver.disconnect();
        });

        benefitChangeObserver.observe(benefitChipList, { childList: true });
    }

    const loginRejectionModal = document.querySelector(
        "[data-login-rejection-modal]"
    );

    logoInput?.addEventListener("change", function () {
        const file = logoInput.files?.[0];

        if (logoMessage) {
            logoMessage.textContent = "";
            logoMessage.classList.remove("is-visible", "is-success");
        }
        if (!file) {
            return;
        }
        if (!["image/jpeg", "image/png"].includes(file.type)) {
            logoInput.value = "";
            showLogoMessage("JPG 또는 PNG 이미지만 등록할 수 있습니다.");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            logoInput.value = "";
            showLogoMessage("기업 로고는 5MB 이하로 등록해주세요.");
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
            showLogoMessage(
                "선택한 로고가 미리보기에 반영되었습니다.",
                true
            );
        });
        reader.readAsDataURL(file);
    });

    function showLogoMessage(message, success = false) {
        if (!logoMessage) {
            return;
        }
        logoMessage.textContent = message;
        logoMessage.classList.add("is-visible");
        logoMessage.classList.toggle("is-success", success);
    }

    if (loginRejectionModal) {
        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "기업 승인 반려 안내",
            message:
                "기업 가입 승인이 반려되었습니다.\n\n반려 사유: "
                + (loginRejectionModal.dataset.rejectionReason
                    || "반려 사유를 확인할 수 없습니다.")
                + "\n\n기업정보를 수정한 뒤 재심사를 요청해주세요.",
            leftText: "닫기",
            rightText: "기업정보 수정",
            leftClass: "btn-outline",
            rightClass: "btn-danger",
            onRight: function () {
                form.scrollIntoView({ behavior: "smooth", block: "start" });
            }
        });

        const queryParameters = new URLSearchParams(window.location.search);
        queryParameters.delete("showRejectionModal");
        const queryString = queryParameters.toString();
        history.replaceState(
            null,
            "",
            window.location.pathname + (queryString ? "?" + queryString : "")
        );
    }

    /* 사업자등록번호 자동 하이픈 */

    businessNumberInput?.addEventListener("input", function () {
        const number = businessNumberInput.value.replace(/[^0-9]/g, "").slice(0, 10);

        if (businessNumberError) {
            businessNumberError.textContent = "";
            businessNumberError.classList.remove("is-visible");
        }

        if (number.length <= 3) {
            businessNumberInput.value = number;
            return;
        }

        if (number.length <= 5) {
            businessNumberInput.value = number.slice(0, 3) + "-" + number.slice(3);

            return;
        }

        businessNumberInput.value = number.slice(0, 3) + "-" + number.slice(3, 5) + "-" + number.slice(5);
    });

    /**
     * 검증 결과를 알린다.
     *
     * 폼 하단의 문구는 화면 맨 아래(복지 아래)에 있어, 검증에 걸린 입력으로 focus가 이동하면
     * 화면 밖으로 밀려나 보이지 않는다. 그래서 고정 위치 토스트를 기본 통로로 쓰고,
     * toast.js를 불러오지 못한 경우에만 기존 문구로 대체한다.
     */
    function showFormMessage(message) {
        if (typeof window.showToast === "function") {
            window.showToast(message, "error");
            return;
        }

        if (!formMessage) {
            return;
        }

        formMessage.textContent = message;
        formMessage.classList.add("is-visible");
    }

    function clearFormMessage() {
        if (!formMessage) {
            return;
        }

        formMessage.textContent = "";
        formMessage.classList.remove("is-visible");
    }

    function validateRejectedFields() {
        clearFormMessage();

        const requiredFields = Array.from(form.querySelectorAll("[required]"));

        const invalidField = requiredFields.find(function (field) {
            return !String(field.value).trim();
        });

        if (invalidField) {
            showFormMessage("반려 사유와 관련된 필수 정보를 모두 입력해주세요.");

            markInvalid(invalidField);
            invalidField.focus();
            return false;
        }

        const businessNumber = businessNumberInput?.value.replace(/[^0-9]/g, "") ?? "";

        if (businessNumber.length !== 10) {
            markInvalid(businessNumberInput);

            if (businessNumberError) {
                businessNumberError.textContent =
                    "사업자등록번호 10자리를 정확히 입력해주세요.";
                businessNumberError.classList.add("is-visible");
            }

            businessNumberInput?.focus();
            return false;
        }

        return true;
    }

    /* 변경 내용 제출 및 재심사 요청 */

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        if (!validateRejectedFields()) {
            return;
        }

        showConfirmModal({
            iconClass: "warning",
            iconHtml: "?",
            title: "재심사를 요청할까요?",
            message:
                "수정한 기업정보가 관리자에게 제출됩니다.\n제출 후에는 심사 결과가 나올 때까지 일부 항목을 수정할 수 없습니다.",
            leftText: "취소",
            rightText: "재심사 요청",
            leftClass: "btn-outline",
            rightClass: "btn-primary",

            onRight: function () {
                form.submit();
            },
        });
    });
});
