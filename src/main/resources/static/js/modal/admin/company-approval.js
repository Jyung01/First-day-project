document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-company-approval-modal]");

    if (!modal) {
        return;
    }

    const openButtons = document.querySelectorAll("[data-company-approval-open]");

    const closeButtons = modal.querySelectorAll("[data-company-approval-close]");

    const form = modal.querySelector("[data-company-approval-form]");

    const resultInputs = Array.from(modal.querySelectorAll("[data-approval-result]"));

    const rejectionSection = modal.querySelector("[data-rejection-reason-section]");

    const rejectionInputs = Array.from(modal.querySelectorAll("[data-rejection-reason]"));

    const messageInput = modal.querySelector("[data-approval-message]");

    const messageCount = modal.querySelector("[data-approval-message-count]");

    const errorMessage = modal.querySelector("[data-company-approval-error]");

    const submitButton = modal.querySelector("[data-company-approval-submit]");

    const companyName = modal.querySelector("[data-company-name]");

    const businessNumber = modal.querySelector("[data-company-business-number]");

    const representative = modal.querySelector("[data-company-representative]");

    const manager = modal.querySelector("[data-company-manager]");

    let selectedCompanyId = null;
    let previousFocus = null;

    function getSelectedResult() {
        return resultInputs.find(function (input) {
            return input.checked;
        })?.value;
    }

    function getSelectedRejectionReason() {
        return rejectionInputs.find(function (input) {
            return input.checked;
        })?.value;
    }

    function clearError() {
        if (errorMessage) {
            errorMessage.textContent = "";
            errorMessage.classList.remove("is-visible");
        }

        messageInput?.classList.remove("is-error");
    }

    function showError(message, target) {
        if (errorMessage) {
            errorMessage.textContent = message;
            errorMessage.classList.add("is-visible");
        }

        target?.classList.add("is-error");
        target?.focus();
    }

    function updateMessageCount() {
        if (!messageInput || !messageCount) {
            return;
        }

        messageCount.textContent = String(messageInput.value.length);
    }

    function updateResultMode() {
        const result = getSelectedResult();
        const rejected = result === "REJECTED";

        if (rejectionSection) {
            rejectionSection.disabled = !rejected;
        }

        if (!rejected) {
            rejectionInputs.forEach(function (input) {
                input.checked = false;
            });
        }

        if (submitButton) {
            submitButton.classList.remove("admin-modal-button--danger", "admin-modal-button--primary");

            if (result === "APPROVED") {
                submitButton.textContent = "승인 처리";
                submitButton.classList.add("admin-modal-button--primary");
            } else if (result === "REJECTED") {
                submitButton.textContent = "반려 처리";
                submitButton.classList.add("admin-modal-button--danger");
            } else {
                submitButton.textContent = "처리";
                submitButton.classList.add("admin-modal-button--danger");
            }
        }

        clearError();
    }

    function resetModal() {
        form?.reset();

        selectedCompanyId = null;

        if (rejectionSection) {
            rejectionSection.disabled = true;
        }

        if (messageInput) {
            messageInput.value = "";
        }

        updateMessageCount();
        updateResultMode();
        clearError();
    }

    function openModal(button) {
        previousFocus = document.activeElement;

        resetModal();

        selectedCompanyId = button.dataset.companyId || null;

        if (companyName) {
            companyName.textContent =
                `${button.dataset.companyName || "-"} · ` + `${button.dataset.reviewType || "신규 심사"}`;
        }

        if (businessNumber) {
            businessNumber.textContent = button.dataset.businessNumber || "-";
        }

        if (representative) {
            representative.textContent = button.dataset.representative || "-";
        }

        if (manager) {
            manager.textContent = button.dataset.manager || "-";
        }

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");

        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";

        resetModal();
        previousFocus?.focus();
    }

    openButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            openModal(button);
        });
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    resultInputs.forEach(function (input) {
        input.addEventListener("change", updateResultMode);
    });

    messageInput?.addEventListener("input", function () {
        clearError();
        updateMessageCount();
    });

    form?.addEventListener("submit", function (event) {
        event.preventDefault();
        clearError();

        const result = getSelectedResult();

        if (!result) {
            showError("승인 또는 반려를 선택해주세요.");

            return;
        }

        const rejectionReason = getSelectedRejectionReason();

        if (result === "REJECTED" && !rejectionReason) {
            showError("반려 사유를 선택해주세요.");

            return;
        }

        const message = messageInput?.value.trim() || "";

        if (result === "REJECTED" && !message) {
            showError("기업회원에게 전달할 관리자 안내를 입력해주세요.", messageInput);

            return;
        }

        const requestData = {
            companyId: selectedCompanyId,
            result,
            rejectionReason: result === "REJECTED" ? rejectionReason : null,
            message,
        };

        /*
         * 실제 구현 시 기업 승인·반려 API 호출
         */

        console.log(requestData);

        closeModal();
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });

    updateMessageCount();
});
