document.addEventListener("DOMContentLoaded", function () {
    const managerForm = document.querySelector("[data-corp-manager-form]");

    const passwordOpenButton = document.querySelector("[data-corp-password-open]");

    const withdrawButton = document.querySelector("[data-corp-withdraw]");

    const rejectionGuideButton = document.querySelector("[data-corp-rejection-guide]");

    initializeManagerForm(managerForm);
    initializePasswordModal(passwordOpenButton);
    initializeWithdrawModal(withdrawButton);

    if (document.querySelector("[data-corp-account-saved]")) {
        showConfirmModal({
            iconClass: "success",
            iconHtml: "✓",
            title: "담당자 정보가 저장되었습니다",
            message: "변경한 부서와 직책이 정상적으로 반영되었습니다.",
            leftVisible: false,
            rightText: "확인",
            rightClass: "btn-primary",
        });
    }

    rejectionGuideButton?.addEventListener("click", showCorpRejectionGuide);

    window.showCorpRejectionGuide = showCorpRejectionGuide;
});

/* =========================================================
   담당자 정보
========================================================= */

function initializeManagerForm(form) {
    if (!form) {
        return;
    }

    const message = form.querySelector("[data-corp-manager-message]");

    function showMessage(text, success = false) {
        if (!message) {
            return;
        }

        message.textContent = text;
        message.classList.add("is-visible");
        message.classList.toggle("is-success", success);
    }

    function clearMessage() {
        if (!message) {
            return;
        }

        message.textContent = "";
        message.classList.remove("is-visible", "is-success");
    }

    form.addEventListener("input", clearMessage);

    form.addEventListener("submit", function (event) {
        clearMessage();

        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
        }
    });
}

/* =========================================================
   비밀번호 변경 form-modal
========================================================= */

function initializePasswordModal(openButton) {
    openButton?.addEventListener("click", function () {
        const bodyHtml = `
            <div class="corp-password-modal-form"
                 data-corp-password-form>

                <div class="corp-password-modal-field">
                    <label for="corpCurrentPassword">
                        현재 비밀번호
                    </label>

                    <div class="corp-password-input-wrap">
                        <input
                            type="password"
                            id="corpCurrentPassword"
                            class="corp-password-modal-input"
                            autocomplete="current-password"
                            placeholder="현재 비밀번호를 입력해주세요"
                            data-corp-current-password
                        />

                        <button
                            type="button"
                            class="corp-password-toggle"
                            data-corp-password-toggle
                            data-target="corpCurrentPassword"
                        >
                            보기
                        </button>
                    </div>
                </div>

                <div class="corp-password-modal-field">
                    <label for="corpNewPassword">
                        새 비밀번호
                    </label>

                    <div class="corp-password-input-wrap">
                        <input
                            type="password"
                            id="corpNewPassword"
                            class="corp-password-modal-input"
                            autocomplete="new-password"
                            placeholder="영문·숫자·특수문자 조합 8자 이상"
                            data-corp-new-password
                        />

                        <button
                            type="button"
                            class="corp-password-toggle"
                            data-corp-password-toggle
                            data-target="corpNewPassword"
                        >
                            보기
                        </button>
                    </div>
                </div>

                <div class="corp-password-modal-field">
                    <label for="corpNewPasswordConfirm">
                        새 비밀번호 확인
                    </label>

                    <div class="corp-password-input-wrap">
                        <input
                            type="password"
                            id="corpNewPasswordConfirm"
                            class="corp-password-modal-input"
                            autocomplete="new-password"
                            placeholder="새 비밀번호를 다시 입력해주세요"
                            data-corp-new-password-confirm
                        />

                        <button
                            type="button"
                            class="corp-password-toggle"
                            data-corp-password-toggle
                            data-target="corpNewPasswordConfirm"
                        >
                            보기
                        </button>
                    </div>
                </div>

                <p
                    class="corp-password-modal-message"
                    data-corp-password-message
                ></p>
            </div>
        `;

        showFormModal({
            title: "비밀번호 변경",
            bodyHtml,
            leftText: "취소",
            rightText: "비밀번호 변경",

            onRight: function () {
                submitPasswordChange();
            },
        });

        initializePasswordToggleButtons();

        window.setTimeout(function () {
            document.querySelector("[data-corp-current-password]")?.focus();
        }, 0);
    });
}

function initializePasswordToggleButtons() {
    document.querySelectorAll("[data-corp-password-toggle]").forEach(function (button) {
        button.addEventListener("click", function () {
            const target = document.getElementById(button.dataset.target);

            if (!target) {
                return;
            }

            const hidden = target.type === "password";

            target.type = hidden ? "text" : "password";

            button.textContent = hidden ? "숨기기" : "보기";
        });
    });
}

async function submitPasswordChange() {
    const currentPassword = document.querySelector("[data-corp-current-password]");

    const newPassword = document.querySelector("[data-corp-new-password]");

    const newPasswordConfirm = document.querySelector("[data-corp-new-password-confirm]");

    const message = document.querySelector("[data-corp-password-message]");

    function showError(text, target) {
        if (message) {
            message.textContent = text;
            message.classList.add("is-visible");
        }

        target?.classList.add("is-error");
        target?.focus();
    }

    document.querySelectorAll(".corp-password-modal-input.is-error").forEach(function (input) {
        input.classList.remove("is-error");
    });

    if (message) {
        message.textContent = "";
        message.classList.remove("is-visible");
    }

    const currentValue = currentPassword?.value.trim() ?? "";

    const newValue = newPassword?.value ?? "";

    const confirmValue = newPasswordConfirm?.value ?? "";

    if (!currentValue) {
        showError("현재 비밀번호를 입력해주세요.", currentPassword);
        return;
    }

    const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

    if (!passwordPattern.test(newValue)) {
        showError("영문·숫자·특수문자를 포함해 8자 이상 입력해주세요.", newPassword);
        return;
    }

    if (newValue !== confirmValue) {
        showError("새 비밀번호가 일치하지 않습니다.", newPasswordConfirm);
        return;
    }

    if (currentValue === newValue) {
        showError("현재 비밀번호와 다른 비밀번호를 입력해주세요.", newPassword);
        return;
    }

    try {
        const response = await fetch("/corp/account/password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentPassword: currentValue,
                newPassword: newValue,
                newPasswordConfirm: confirmValue,
            }),
        });
        const result = await readJsonResponse(response);
        if (!response.ok || !result.success) {
            showError(result.message || "비밀번호를 변경하지 못했습니다.", currentPassword);
            return;
        }

        closeModal("formModal");
        const changedAt = document.querySelector("[data-password-changed-at]");
        if (changedAt) {
            const today = new Intl.DateTimeFormat("ko-KR", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
            }).format(new Date()).replaceAll(". ", ".").replace(/\.$/, "");
            changedAt.textContent = `마지막 변경 ${today}`;
        }
        showConfirmModal({
            iconClass: "success",
            iconHtml: "✓",
            title: "비밀번호가 변경되었습니다",
            message: "다음 로그인부터 새로운 비밀번호를 사용해주세요.",
            leftVisible: false,
            rightText: "확인",
            rightClass: "btn-primary",
        });
    } catch (error) {
        showError("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", currentPassword);
    }
}

/* =========================================================
   기업회원 탈퇴 확인
========================================================= */

function initializeWithdrawModal(button) {
    button?.addEventListener("click", function () {
        const activeJobCount = button.dataset.activeJobCount || "0";

        const applicantCount = button.dataset.applicantCount || "0";

        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "기업회원 탈퇴 확인",
            message: "탈퇴 후 기업 페이지와 진행 중 공고는 공개되지 않습니다.",

            extraHtml: `
                <div class="corp-modal-summary">
                    <strong>
                        모집 중 공고 ${activeJobCount}건 ·
                        전형 진행 중 지원자 ${applicantCount}명
                    </strong>

                    <p>
                        모집 중 공고는 자동 마감되며 기존 지원 내역과
                        제출 문서는 관련 정책에 따라 보관됩니다.
                    </p>
                </div>
            `,

            leftVisible: false,
            rightText: "확인",
            rightClass: "btn-outline",

            onRight: function () {
                showFinalWithdrawModal();
            },
        });
    });
}

function showFinalWithdrawModal() {
    const bodyHtml = `
        <div class="corp-password-modal-form">
            <p class="corp-modal-withdraw-guide">
                탈퇴 처리는 되돌릴 수 없습니다.<br />
                계속하려면 현재 비밀번호를 입력해주세요.
            </p>
            <div class="corp-password-modal-field">
                <label for="corpWithdrawPassword">현재 비밀번호</label>
                <div class="corp-password-input-wrap">
                    <input
                        type="password"
                        id="corpWithdrawPassword"
                        class="corp-password-modal-input"
                        autocomplete="current-password"
                        data-corp-withdraw-password
                    />
                    <button
                        type="button"
                        class="corp-password-toggle"
                        data-corp-password-toggle
                        data-target="corpWithdrawPassword"
                    >보기</button>
                </div>
            </div>
            <p class="corp-password-modal-message" data-corp-withdraw-message></p>
        </div>
    `;
    showFormModal({
        title: "정말 기업회원에서 탈퇴할까요?",
        bodyHtml,
        leftText: "취소",
        rightText: "기업회원 탈퇴",
        leftClass: "btn-outline",
        rightClass: "btn-danger",
        onRight: function () {
            submitCompanyWithdrawal();
        },
    });
    initializePasswordToggleButtons();
    window.setTimeout(function () {
        document.querySelector("[data-corp-withdraw-password]")?.focus();
    }, 0);
}

async function submitCompanyWithdrawal() {
    const passwordInput = document.querySelector("[data-corp-withdraw-password]");
    const message = document.querySelector("[data-corp-withdraw-message]");
    const currentPassword = passwordInput?.value ?? "";

    function showError(text) {
        if (message) {
            message.textContent = text;
            message.classList.add("is-visible");
        }
        passwordInput?.classList.add("is-error");
        passwordInput?.focus();
    }

    if (!currentPassword) {
        showError("현재 비밀번호를 입력해주세요.");
        return;
    }

    try {
        const response = await fetch("/corp/account/withdraw", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ currentPassword }),
        });
        const result = await readJsonResponse(response);
        if (!response.ok || !result.success) {
            showError(result.message || "기업회원 탈퇴를 처리하지 못했습니다.");
            return;
        }
        window.location.href = result.redirectUrl || "/auth/login?accountStatus=withdrawn";
    } catch (error) {
        showError("요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}

async function readJsonResponse(response) {
    try {
        return await response.json();
    } catch (error) {
        return {
            success: false,
            message: response.status === 401
                ? "로그인이 만료되었습니다. 다시 로그인해주세요."
                : "서버 응답을 확인할 수 없습니다.",
        };
    }
}

/* =========================================================
   기업 승인 반려 안내
========================================================= */

function showCorpRejectionGuide() {
    showConfirmModal({
        iconClass: "danger",
        iconHtml: "!",
        title: "기업 승인 반려 안내",
        message: "기업 가입 승인이 반려되었습니다.",

        extraHtml: `
            <div class="corp-modal-summary">
                <strong>
                    반려 사유: 형식 오류
                </strong>

                <p>
                    기업정보를 수정한 뒤 재심사를 요청해주세요.
                </p>
            </div>

            <div class="corp-modal-guide">
                수정 화면에서 반려 항목이 강조됩니다.
            </div>
        `,

        leftText: "닫기",
        rightText: "기업정보 수정",
        leftClass: "btn-outline",
        rightClass: "btn-danger",

        onRight: function () {
            window.location.href = "/corp/company-info-rejected";
        },
    });
}
