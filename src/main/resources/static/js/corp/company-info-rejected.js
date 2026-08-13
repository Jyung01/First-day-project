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

    function showFormMessage(message) {
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

            invalidField.classList.add("is-rejected");
            invalidField.focus();
            return false;
        }

        const businessNumber = businessNumberInput?.value.replace(/[^0-9]/g, "") ?? "";

        if (businessNumber.length !== 10) {
            businessNumberInput?.classList.add("is-rejected");

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
