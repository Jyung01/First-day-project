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

    nameInput?.addEventListener("input", function () {
        clearAccountMessage(message);
        result.hidden = true;
    });

    emailInput?.addEventListener("input", function () {
        clearAccountMessage(message);
        result.hidden = true;
    });

    form.addEventListener("submit", function (event) {
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

        /*
         * 추후 아이디 조회 API로 교체
         *
         * 예시:
         * fetch("/auth/find-id", {
         *     method: "POST",
         *     headers: {
         *         "Content-Type": "application/json"
         *     },
         *     body: JSON.stringify({ name, email })
         * });
         */

        const matched = name === "홍길동" || email.includes("@");

        if (!matched) {
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

            return;
        }

        clearAccountMessage(message);
        result.hidden = false;
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

    let verificationCodeSent = false;

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

    sendCodeButton?.addEventListener("click", function () {
        const memberId = memberIdInput?.value.trim() ?? "";

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

        /*
         * 추후 인증번호 발송 API로 교체
         */
        verificationCodeSent = true;

        showAccountMessage(
            emailMessage,
            "인증번호를 발송했습니다.",
            true
        );

        codeInput?.focus();
    });

    memberIdInput?.addEventListener("input", function () {
        verificationCodeSent = false;
        clearAccountMessage(emailMessage);
    });

    emailInput?.addEventListener("input", function () {
        verificationCodeSent = false;
        clearAccountMessage(emailMessage);
    });

    newPassword?.addEventListener("input", validatePassword);
    newPasswordConfirm?.addEventListener("input", validatePassword);

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        if (!verificationCodeSent) {
            showAccountMessage(
                emailMessage,
                "먼저 인증번호를 받아주세요."
            );
            emailInput?.focus();
            return;
        }

        const code = codeInput?.value.trim() ?? "";

        if (!/^\d{6}$/.test(code)) {
            showAccountMessage(
                emailMessage,
                "인증번호 6자리를 입력해주세요."
            );
            codeInput?.focus();
            return;
        }

        if (!validatePassword()) {
            newPasswordConfirm?.focus();
            return;
        }

        /*
         * 기존 비밀번호와 같은지 여부는
         * 서버에서 현재 암호화된 비밀번호와 비교해야 한다.
         */

        showConfirmModal({
            iconClass: "success",
            iconHtml: "✓",
            title: "비밀번호가 변경되었습니다",
            message:
                "새로운 비밀번호로 로그인해주세요.",
            leftText: "",
            rightText: "로그인으로 이동",
            leftClass: "btn-outline",
            rightClass: "btn-primary",
            onRight: function () {
                window.location.href = "/auth/login";
            }
        });
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