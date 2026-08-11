document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-company-approval-modal]");

    if (!modal) {
        return;
    }

    const openButtons = document.querySelectorAll("[data-company-detail-open]");
    const closeButtons = modal.querySelectorAll("[data-company-approval-close]");
    const form = modal.querySelector("[data-company-approval-form]");
    const reviewControls = modal.querySelector("[data-company-review-controls]");
    const reviewResult = modal.querySelector("[data-company-review-result]");
    const resultInputs = Array.from(modal.querySelectorAll("[data-approval-result]"));
    const rejectionSection = modal.querySelector("[data-rejection-reason-section]");
    const rejectionInputs = Array.from(modal.querySelectorAll("[data-rejection-reason]"));
    const messageInput = modal.querySelector("[data-approval-message]");
    const messageCount = modal.querySelector("[data-approval-message-count]");
    const errorMessage = modal.querySelector("[data-company-approval-error]");
    const submitButton = modal.querySelector("[data-company-approval-submit]");
    const statusGuide = modal.querySelector("[data-company-status-guide]");

    const fields = {
        name: modal.querySelector("[data-company-name]"),
        number: modal.querySelector("[data-company-number]"),
        reviewType: modal.querySelector("[data-company-review-type]"),
        status: modal.querySelector("[data-company-status]"),
        businessNumber: modal.querySelector("[data-company-business-number]"),
        representative: modal.querySelector("[data-company-representative]"),
        establishedDate: modal.querySelector("[data-company-established-date]"),
        industry: modal.querySelector("[data-company-industry]"),
        companySize: modal.querySelector("[data-company-size]"),
        requestedAt: modal.querySelector("[data-company-requested-at]"),
        withdrawnField: modal.querySelector("[data-company-withdrawn-field]"),
        withdrawnAt: modal.querySelector("[data-company-withdrawn-at]"),
        address: modal.querySelector("[data-company-address]"),
        homepage: modal.querySelector("[data-company-homepage]"),
        manager: modal.querySelector("[data-company-manager]"),
        managerEmail: modal.querySelector("[data-company-manager-email]"),
        managerPhone: modal.querySelector("[data-company-manager-phone]"),
        shortDescription: modal.querySelector("[data-company-short-description]"),
        introduction: modal.querySelector("[data-company-introduction]"),
        benefits: modal.querySelector("[data-company-benefits]"),
        rejectionReason: modal.querySelector("[data-company-rejection-reason]"),
        reviewedAt: modal.querySelector("[data-company-reviewed-at]"),
        rejectionMessage: modal.querySelector("[data-company-rejection-message]"),
    };

    const rejectionMessages = {
        MISSING_INFORMATION: "필수 기업정보가 누락되었습니다. 누락된 항목을 입력한 뒤 재심사를 요청해주세요.",
        FORMAT_ERROR: "사업자등록번호 등 입력 형식을 다시 확인한 뒤 재심사를 요청해주세요.",
        INAPPROPRIATE_INFORMATION: "서비스에 공개하기 어려운 기업정보를 수정한 뒤 재심사를 요청해주세요.",
    };

    const statusSettings = {
        PENDING: {
            label: "승인 대기",
            className: "is-pending",
            guide: "승인 또는 반려 처리 전 기업정보를 다시 확인해주세요.",
        },
        APPROVED: {
            label: "승인",
            className: "is-approved",
            actionLabel: "이용정지",
            actionClass: "admin-modal-button--danger",
            guide: "이용정지하면 기업회원의 로그인과 기업관리 서비스 이용이 제한됩니다.",
        },
        REJECTED: {
            label: "반려",
            className: "is-rejected",
            guide: "기업이 정보를 수정해 재심사를 요청하면 승인 대기 목록에 다시 표시됩니다.",
        },
        SUSPENDED: {
            label: "이용정지",
            className: "is-suspended",
            actionLabel: "정지 해제",
            actionClass: "admin-modal-button--primary",
            guide: "정지를 해제하면 기업회원이 다시 로그인하여 서비스를 이용할 수 있습니다.",
        },
        WITHDRAWN: {
            label: "탈퇴",
            className: "is-withdrawn",
            guide: "탈퇴한 기업은 이력 조회만 가능하며 상태를 변경할 수 없습니다.",
        },
    };

    let selectedCompanyId = null;
    let selectedCompanyName = null;
    let selectedCompanyStatus = "PENDING";
    let selectedCompanyDetailUrl = null;
    let previousFocus = null;
    let lastDefaultMessage = "";
    let requestSequence = 0;

    function setText(element, value) {
        if (element) {
            element.textContent = value == null || String(value).trim() === ""
                ? "-"
                : String(value).trim();
        }
    }

    function formatDate(value, includeTime = false) {
        if (!value) {
            return "";
        }

        const [date, time = ""] = value.split("T");
        const formattedDate = date.replaceAll("-", ".");
        return includeTime && time
            ? `${formattedDate} ${time.slice(0, 5)}`
            : formattedDate;
    }

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
        if (messageInput && messageCount) {
            messageCount.textContent = String(messageInput.value.length);
        }
    }

    function updateRejectionMessage() {
        if (!messageInput) {
            return;
        }

        const reason = getSelectedRejectionReason();
        const nextDefaultMessage = reason ? rejectionMessages[reason] : "";
        const canReplace = !messageInput.value.trim() || messageInput.value === lastDefaultMessage;

        if (canReplace) {
            messageInput.value = nextDefaultMessage;
        }

        lastDefaultMessage = nextDefaultMessage;
        updateMessageCount();
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
            lastDefaultMessage = "";
        }

        if (result === "APPROVED" && messageInput) {
            messageInput.value = "";
            updateMessageCount();
        }

        if (submitButton && selectedCompanyStatus === "PENDING") {
            submitButton.textContent = result === "APPROVED"
                ? "승인 처리"
                : result === "REJECTED"
                    ? "반려 처리"
                    : "처리";
            submitButton.classList.toggle("admin-modal-button--primary", result === "APPROVED");
            submitButton.classList.toggle("admin-modal-button--danger", result !== "APPROVED");
        }

        clearError();
    }

    function renderStatus(statusCode) {
        selectedCompanyStatus = statusSettings[statusCode] ? statusCode : "PENDING";
        const setting = statusSettings[selectedCompanyStatus];

        fields.status.classList.remove(
            "is-pending",
            "is-approved",
            "is-rejected",
            "is-suspended",
            "is-withdrawn"
        );
        fields.status.classList.add(setting.className);
        fields.status.textContent = setting.label;
        statusGuide.textContent = setting.guide;

        reviewControls.hidden = selectedCompanyStatus !== "PENDING";
        reviewResult.hidden = selectedCompanyStatus !== "REJECTED";
        fields.withdrawnField.hidden = selectedCompanyStatus !== "WITHDRAWN";
        submitButton.hidden = ["REJECTED", "WITHDRAWN"].includes(selectedCompanyStatus);
        submitButton.classList.remove("admin-modal-button--danger", "admin-modal-button--primary");

        if (selectedCompanyStatus === "PENDING") {
            submitButton.textContent = "처리";
            submitButton.classList.add("admin-modal-button--danger");
            return;
        }

        if (setting.actionLabel) {
            submitButton.textContent = setting.actionLabel;
            submitButton.classList.add(setting.actionClass);
        }
    }

    function resetModal() {
        form?.reset();
        selectedCompanyId = null;
        selectedCompanyName = null;
        selectedCompanyDetailUrl = null;
        lastDefaultMessage = "";

        if (rejectionSection) {
            rejectionSection.disabled = true;
        }

        if (messageInput) {
            messageInput.value = "";
        }

        updateMessageCount();
        clearError();
    }

    function renderCompany(company) {
        selectedCompanyId = company.companyId || null;
        selectedCompanyName = company.companyName || "기업";

        setText(fields.name, company.companyName);
        setText(fields.number, company.companyNumber);
        setText(fields.reviewType, company.reviewTypeLabel);
        setText(fields.businessNumber, company.businessNumber);
        setText(fields.representative, company.representativeName);
        setText(fields.establishedDate, formatDate(company.establishedDate));
        setText(fields.industry, company.industryName);
        setText(fields.companySize, company.companySize);
        setText(fields.requestedAt, formatDate(company.requestedAt, true));
        setText(fields.withdrawnAt, formatDate(company.withdrawnAt, true));
        setText(fields.address, company.address);
        setText(fields.homepage, company.homepageUrl);
        setText(fields.manager, company.managerName);
        setText(fields.managerEmail, company.managerEmail);
        setText(fields.managerPhone, company.managerPhone);
        setText(fields.shortDescription, company.shortDescription);
        setText(fields.introduction, company.introduction);
        setText(fields.benefits, company.benefits);
        setText(fields.rejectionReason, company.rejectionLabel);
        setText(fields.reviewedAt, formatDate(company.reviewedAt, true));
        setText(fields.rejectionMessage, company.rejectionReason);
        renderStatus(company.statusCode);
    }

    function showModal() {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }

    function renderLoading() {
        Object.values(fields).forEach(function (field) {
            if (field !== fields.withdrawnField) {
                setText(field, "불러오는 중...");
            }
        });
        fields.withdrawnField.hidden = true;
        reviewControls.hidden = true;
        reviewResult.hidden = true;
        submitButton.hidden = true;
        statusGuide.textContent = "기업 정보를 불러오고 있습니다.";
        modal.setAttribute("aria-busy", "true");
    }

    function renderLoadError() {
        Object.values(fields).forEach(function (field) {
            if (field !== fields.withdrawnField) {
                setText(field, "-");
            }
        });
        fields.status.textContent = "조회 실패";
        fields.withdrawnField.hidden = true;
        reviewControls.hidden = true;
        reviewResult.hidden = true;
        submitButton.hidden = true;
        statusGuide.textContent = "기업 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
    }

    async function openModal(button) {
        const detailUrl = button.dataset.companyDetailUrl;
        const currentRequest = ++requestSequence;

        previousFocus = document.activeElement;
        resetModal();
        selectedCompanyDetailUrl = detailUrl;
        renderLoading();
        showModal();

        try {
            const response = await fetch(detailUrl, {
                headers: { Accept: "application/json" },
            });

            if (!response.ok) {
                throw new Error(`기업 상세 조회 실패: ${response.status}`);
            }

            const company = await response.json();
            if (currentRequest === requestSequence) {
                renderCompany(company);
            }
        } catch (error) {
            if (currentRequest === requestSequence) {
                renderLoadError();
                console.error(error);
            }
        } finally {
            if (currentRequest === requestSequence) {
                modal.removeAttribute("aria-busy");
            }
        }
    }

    function closeModal() {
        requestSequence += 1;
        selectedCompanyDetailUrl = null;
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        modal.removeAttribute("aria-busy");
        document.body.style.overflow = "";
        resetModal();
        previousFocus?.focus();
    }

    function confirmPendingReview() {
        const result = getSelectedResult();

        if (!result) {
            showError("승인 또는 반려를 선택해주세요.");
            return null;
        }

        const rejectionReason = getSelectedRejectionReason();
        const message = messageInput?.value.trim() || "";

        if (result === "REJECTED" && !rejectionReason) {
            showError("반려 사유를 선택해주세요.");
            return null;
        }

        if (result === "REJECTED" && !message) {
            showError("기업회원에게 전달할 관리자 안내를 입력해주세요.", messageInput);
            return null;
        }

        const actionLabel = result === "APPROVED" ? "승인" : "반려";
        if (!window.confirm(`${selectedCompanyName}을(를) ${actionLabel} 처리하시겠습니까?`)) {
            return null;
        }

        return result === "APPROVED"
            ? { action: "approve", body: null }
            : {
                action: "reject",
                body: {
                    rejectionCode: rejectionReason,
                    message,
                },
            };
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

    rejectionInputs.forEach(function (input) {
        input.addEventListener("change", updateRejectionMessage);
    });

    messageInput?.addEventListener("input", function () {
        clearError();
        updateMessageCount();
    });

    form?.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearError();

        let requestData;

        if (selectedCompanyStatus === "PENDING") {
            requestData = confirmPendingReview();
        } else if (selectedCompanyStatus === "APPROVED") {
            if (!window.confirm(`${selectedCompanyName}을(를) 이용정지하시겠습니까?`)) {
                return;
            }
            requestData = { action: "suspend", body: null };
        } else if (selectedCompanyStatus === "SUSPENDED") {
            if (!window.confirm(`${selectedCompanyName}의 이용정지를 해제하시겠습니까?`)) {
                return;
            }
            requestData = { action: "unsuspend", body: null };
        }

        if (!requestData) {
            return;
        }

        if (!selectedCompanyDetailUrl) {
            showError("기업 요청 정보를 확인할 수 없습니다. 목록을 새로고침해 주세요.");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "처리 중...";

        try {
            const options = {
                method: "POST",
                headers: { Accept: "application/json" },
            };

            if (requestData.body) {
                options.headers["Content-Type"] = "application/json";
                options.body = JSON.stringify(requestData.body);
            }

            const response = await fetch(
                `${selectedCompanyDetailUrl}/${requestData.action}`,
                options
            );

            if (!response.ok) {
                const message = response.status === 400
                    ? "반려 사유와 관리자 안내를 다시 확인해 주세요."
                    : response.status === 404
                        ? "기업 정보를 찾을 수 없습니다. 목록을 새로고침해 주세요."
                        : response.status === 409
                            ? "기업 상태가 이미 변경되었습니다. 목록을 새로고침해 주세요."
                            : "기업 상태를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.";
                throw new Error(message);
            }

            window.location.reload();
        } catch (error) {
            console.error(error);
            submitButton.disabled = false;
            renderStatus(selectedCompanyStatus);
            updateResultMode();
            showError(error.message || "기업 상태를 변경하지 못했습니다.");
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });

    updateMessageCount();
});
