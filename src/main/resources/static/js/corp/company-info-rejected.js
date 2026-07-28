document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-rejected-company-form]");

    if (!form) {
        return;
    }

    const reviewButton = document.querySelector("[data-review-request]");

    const submitModeInput = form.querySelector("[data-submit-mode-input]");

    const formMessage = form.querySelector("[data-rejected-form-message]");

    const businessNumberInput = form.querySelector("[data-business-number]");

    /* 사업자등록번호 자동 하이픈 */

    businessNumberInput?.addEventListener("input", function () {
        const number = businessNumberInput.value.replace(/[^0-9]/g, "").slice(0, 10);

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

            invalidField.focus();
            return false;
        }

        const businessNumber = businessNumberInput?.value.replace(/[^0-9]/g, "") ?? "";

        if (businessNumber.length !== 10) {
            showFormMessage("사업자등록번호 10자리를 정확히 입력해주세요.");

            businessNumberInput?.focus();
            return false;
        }

        return true;
    }

    /* 수정 저장 */

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        if (!validateRejectedFields()) {
            return;
        }

        if (submitModeInput) {
            submitModeInput.value = "save";
        }

        /*
         * 실제 구현 시:
         * POST /corp/company-info-rejected
         * submitMode=save
         */

        showConfirmModal({
            iconClass: "success",
            iconHtml: "✓",
            title: "수정 내용이 저장되었습니다",
            message: "재심사를 요청하기 전까지 기업 승인 상태는 반려로 유지됩니다.",
            leftVisible: false,
            rightText: "확인",
            rightClass: "btn-primary",
        });
    });

    /* 재심사 요청 */

    reviewButton?.addEventListener("click", function () {
        if (!validateRejectedFields()) {
            return;
        }

        showConfirmModal({
            iconClass: "warning",
            iconHtml: "!",
            title: "재심사를 요청할까요?",
            message:
                "수정한 기업정보가 관리자에게 제출됩니다.\n제출 후에는 심사 결과가 나올 때까지 일부 항목을 수정할 수 없습니다.",
            leftText: "취소",
            rightText: "재심사 요청",
            leftClass: "btn-outline",
            rightClass: "btn-primary",

            onRight: function () {
                if (submitModeInput) {
                    submitModeInput.value = "review";
                }

                /*
                 * 실제 POST 구현 후:
                 * form.submit();
                 */

                showConfirmModal({
                    iconClass: "success",
                    iconHtml: "✓",
                    title: "재심사가 요청되었습니다",
                    message: "관리자 검토가 완료되면 승인 상태가 변경됩니다.",
                    leftVisible: false,
                    rightText: "확인",
                    rightClass: "btn-primary",
                });
            },
        });
    });
});
