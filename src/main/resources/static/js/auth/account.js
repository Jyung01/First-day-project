document.addEventListener("DOMContentLoaded", function () {
    initializeFindId();
    initializeResetPassword();
    initializePasswordToggle();
});

/* =========================================================
   공통 메시지
========================================================= */

function showAccountMessage(element, message, success = false) {
    if (!element) {
        return;
    }

    element.textContent = message;
    element.classList.add("is-visible");
    element.classList.toggle("is-success", success);
}

function clearAccountMessage(element) {
    if (!element) {
        return;
    }

    element.textContent = "";
    element.classList.remove("is-visible", "is-success");
}

async function postAccountJson(url, body) {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    });
    const data = await response.json().catch(function () {
        return {};
    });

    if (!response.ok) {
        throw new Error(data.message || "요청을 처리하지 못했습니다.");
    }
    return data;
}

/* =========================================================
   아이디 찾기
========================================================= */

function initializeFindId() {
    const form = document.querySelector("[data-find-id-form]");

    if (!form) {
        return;
    }

    const nameInput = form.querySelector("[data-find-name]");
    const emailInput = form.querySelector("[data-find-email]");
    const message = form.querySelector("[data-find-message]");
    const result = document.querySelector("[data-find-result]");
    const maskedId = result?.querySelector("[data-masked-id]");
    const joinedDate = result?.querySelector("[data-joined-date]");
    const submitButton = form.querySelector("[type='submit']");

    nameInput?.addEventListener("input", function () {
        clearAccountMessage(message);
        result.hidden = true;
    });

    emailInput?.addEventListener("input", function () {
        clearAccountMessage(message);
        result.hidden = true;
    });

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const name = nameInput?.value.trim() ?? "";
        const email = emailInput?.value.trim() ?? "";

        if (!name) {
            showAccountMessage(message, "이름을 입력해주세요.");
            nameInput?.focus();
            return;
        }

        if (!emailInput?.checkValidity()) {
            showAccountMessage(message, "올바른 이메일을 입력해주세요.");
            emailInput?.focus();
            return;
        }

        submitButton.disabled = true;
        try {
            const data = await postAccountJson("/api/auth/find-id", {
                name,
                email
            });

            if (data.found) {
                clearAccountMessage(message);
                maskedId.textContent = data.maskedLoginId;
                joinedDate.textContent = data.joinedDate;
                result.hidden = false;
                return;
            }

            showConfirmModal({
                iconClass: "danger",
                iconHtml: "!",
                title: "가입 정보를 찾을 수 없습니다",
                message:
                    "입력한 이름과 이메일을 다시 확인해주세요.\n탈퇴한 계정은 조회할 수 없습니다.",
                leftText: "닫기",
                rightText: "회원가입",
                leftClass: "btn-outline",
                rightClass: "btn-primary",
                onRight: function () {
                    window.location.href = "/auth/member-type";
                }
            });
        } catch (error) {
            showAccountMessage(
                message,
                error.message || "아이디 조회 중 오류가 발생했습니다."
            );
        } finally {
            submitButton.disabled = false;
        }
    });
}

/* =========================================================
   비밀번호 재설정
========================================================= */

function initializeResetPassword() {
    const form = document.querySelector("[data-reset-password-form]");

    if (!form) {
        return;
    }

    const memberIdInput = form.querySelector("[data-reset-id]");
    const emailInput = form.querySelector("[data-reset-email]");
    const codeInput = form.querySelector("[data-reset-code]");
    const sendCodeButton = form.querySelector("[data-send-reset-code]");
    const verifyCodeButton = form.querySelector("[data-verify-reset-code]");

    const newPassword = form.querySelector("[data-new-password]");
    const newPasswordConfirm = form.querySelector(
        "[data-new-password-confirm]"
    );

    const emailMessage = form.querySelector(
        "[data-reset-email-message]"
    );
    const passwordMessage = form.querySelector(
        "[data-reset-password-message]"
    );
    const submitButton = form.querySelector("[type='submit']");

    let verificationCodeSent = false;
    let emailVerified = false;

    function setPasswordFieldsEnabled(enabled) {
        newPassword.disabled = !enabled;
        newPasswordConfirm.disabled = !enabled;
        submitButton.disabled = !enabled;
    }

    function resetEmailVerification() {
        verificationCodeSent = false;
        emailVerified = false;
        codeInput.value = "";
        codeInput.disabled = true;
        verifyCodeButton.disabled = true;
        setPasswordFieldsEnabled(false);
    }

    codeInput.disabled = true;
    verifyCodeButton.disabled = true;
    setPasswordFieldsEnabled(false);

    function validatePassword() {
        const password = newPassword?.value ?? "";
        const confirmPassword = newPasswordConfirm?.value ?? "";

        if (!confirmPassword) {
            clearAccountMessage(passwordMessage);
            return false;
        }

        const passwordPattern =
            /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

        if (!passwordPattern.test(password)) {
            showAccountMessage(
                passwordMessage,
                "영문·숫자·특수문자를 포함해 8자 이상 입력해주세요."
            );
            return false;
        }

        if (password !== confirmPassword) {
            showAccountMessage(
                passwordMessage,
                "비밀번호가 일치하지 않습니다."
            );
            return false;
        }

        showAccountMessage(
            passwordMessage,
            "비밀번호가 일치합니다.",
            true
        );

        return true;
    }

    sendCodeButton?.addEventListener("click", async function () {
        const memberId = memberIdInput?.value.trim() ?? "";
        const email = emailInput?.value.trim() ?? "";

        if (!memberId) {
            showAccountMessage(
                emailMessage,
                "아이디를 입력해주세요."
            );
            memberIdInput?.focus();
            return;
        }

        if (!emailInput?.checkValidity()) {
            showAccountMessage(
                emailMessage,
                "올바른 이메일을 입력해주세요."
            );
            emailInput?.focus();
            return;
        }

        sendCodeButton.disabled = true;
        try {
            const data = await postAccountJson(
                "/api/auth/password-reset/code",
                { loginId: memberId, email }
            );
            verificationCodeSent = true;
            emailVerified = false;
            codeInput.disabled = false;
            verifyCodeButton.disabled = false;
            setPasswordFieldsEnabled(false);
            showAccountMessage(emailMessage, data.message, true);
            codeInput?.focus();
        } catch (error) {
            resetEmailVerification();
            showAccountMessage(emailMessage, error.message);
        } finally {
            sendCodeButton.disabled = false;
        }
    });

    memberIdInput?.addEventListener("input", function () {
        resetEmailVerification();
        clearAccountMessage(emailMessage);
    });

    emailInput?.addEventListener("input", function () {
        resetEmailVerification();
        clearAccountMessage(emailMessage);
    });

    codeInput?.addEventListener("input", function () {
        clearAccountMessage(emailMessage);
    });

    verifyCodeButton?.addEventListener("click", async function () {
        if (!verificationCodeSent) {
            showAccountMessage(emailMessage, "먼저 인증번호를 받아주세요.");
            return;
        }

        const email = emailInput?.value.trim() ?? "";
        const code = codeInput?.value.trim() ?? "";
        if (!/^\d{6}$/.test(code)) {
            showAccountMessage(
                emailMessage,
                "인증번호 6자리를 입력해주세요."
            );
            codeInput?.focus();
            return;
        }

        verifyCodeButton.disabled = true;
        try {
            const data = await postAccountJson(
                "/api/email-verifications/verify",
                { email, code }
            );
            emailVerified = true;
            memberIdInput.disabled = true;
            emailInput.disabled = true;
            codeInput.disabled = true;
            sendCodeButton.disabled = true;
            setPasswordFieldsEnabled(true);
            showAccountMessage(emailMessage, data.message, true);
            newPassword?.focus();
        } catch (error) {
            emailVerified = false;
            verifyCodeButton.disabled = false;
            setPasswordFieldsEnabled(false);
            showAccountMessage(emailMessage, error.message);
        }
    });

    newPassword?.addEventListener("input", validatePassword);
    newPasswordConfirm?.addEventListener("input", validatePassword);

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        if (!emailVerified) {
            showAccountMessage(
                emailMessage,
                "이메일 인증을 먼저 완료해주세요."
            );
            return;
        }

        if (!validatePassword()) {
            newPasswordConfirm?.focus();
            return;
        }

        const loginId = memberIdInput?.value.trim() ?? "";
        const email = emailInput?.value.trim() ?? "";

        submitButton.disabled = true;
        try {
            await postAccountJson("/api/auth/password-reset", {
                loginId,
                email,
                newPassword: newPassword.value,
                newPasswordConfirm: newPasswordConfirm.value
            });

            showConfirmModal({
                iconClass: "success",
                iconHtml: "✓",
                title: "비밀번호가 변경되었습니다",
                message: "새로운 비밀번호로 로그인해주세요.",
                leftVisible: false,
                rightText: "로그인으로 이동",
                rightClass: "btn-primary",
                onRight: function () {
                    window.location.href = "/auth/login";
                }
            });
        } catch (error) {
            showAccountMessage(
                emailVerified ? passwordMessage : emailMessage,
                error.message || "비밀번호 변경 중 오류가 발생했습니다."
            );
        } finally {
            submitButton.disabled = !emailVerified;
        }
    });
}

/* =========================================================
   비밀번호 보기
========================================================= */

function initializePasswordToggle() {
    const toggleButtons = document.querySelectorAll(
        "[data-password-toggle]"
    );

    toggleButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const targetId = button.dataset.target;
            const targetInput = document.getElementById(targetId);

            if (!targetInput) {
                return;
            }

            const isPassword = targetInput.type === "password";

            targetInput.type = isPassword ? "text" : "password";
            button.textContent = isPassword ? "숨기기" : "보기";
        });
    });
}
