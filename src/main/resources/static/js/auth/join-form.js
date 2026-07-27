document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-join-form]");

    if (!form) {
        return;
    }

    const idInput = form.querySelector("[data-id-input]");
    const idCheckButton = form.querySelector("[data-id-check]");
    const idMessage = form.querySelector("[data-id-message]");

    const password = form.querySelector("[data-password]");
    const passwordConfirm = form.querySelector("[data-password-confirm]");
    const passwordMessage = form.querySelector("[data-password-message]");

    const phoneInput = form.querySelector("[data-phone-input]");

    const emailInput = form.querySelector("[data-email-input]");
    const sendCodeButton = form.querySelector("[data-send-code]");
    const resendCodeButton = form.querySelector("[data-resend-code]");
    const verificationCode = form.querySelector("[data-verification-code]");
    const confirmCodeButton = form.querySelector("[data-confirm-code]");
    const emailMessage = form.querySelector("[data-email-message]");
    const signupCancelLink = document.querySelector("[data-signup-cancel]");


    let idChecked = false;
    let emailVerified = false;

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

    function validatePasswordMatch() {
        if (!password || !passwordConfirm || !passwordMessage) {
            return true;
        }

        if (!passwordConfirm.value) {
            clearMessage(passwordMessage);
            return false;
        }

        const matched = password.value === passwordConfirm.value;

        showMessage(
            passwordMessage,
            matched
                ? "비밀번호가 일치합니다."
                : "비밀번호가 일치하지 않습니다.",
            matched
        );

        return matched;
    }

    idInput?.addEventListener("input", function () {
        idChecked = false;
        clearMessage(idMessage);
    });

    idCheckButton?.addEventListener("click", function () {
        const memberId = idInput?.value.trim() ?? "";
        const valid = /^[a-zA-Z0-9]{6,20}$/.test(memberId);

        if (!valid) {
            idChecked = false;

            showMessage(
                idMessage,
                "아이디는 영문과 숫자를 사용해 6~20자로 입력해주세요."
            );

            idInput?.focus();
            return;
        }

        /*
         * 추후 서버 중복확인 API 호출로 교체
         */
        idChecked = true;
        showMessage(idMessage, "사용 가능한 아이디입니다.", true);
    });

    password?.addEventListener("input", validatePasswordMatch);
    passwordConfirm?.addEventListener("input", validatePasswordMatch);

    phoneInput?.addEventListener("input", function () {
        const number = phoneInput.value
            .replace(/[^0-9]/g, "")
            .slice(0, 11);

        if (number.length <= 3) {
            phoneInput.value = number;
            return;
        }

        if (number.length <= 7) {
            phoneInput.value =
                number.slice(0, 3) + "-" + number.slice(3);
            return;
        }

        phoneInput.value =
            number.slice(0, 3) +
            "-" +
            number.slice(3, 7) +
            "-" +
            number.slice(7);
    });

    emailInput?.addEventListener("input", function () {
        emailVerified = false;
        clearMessage(emailMessage);
    });

    function sendVerificationCode() {
        if (!emailInput?.checkValidity()) {
            emailInput?.reportValidity();
            return;
        }

        /*
         * 추후 이메일 인증번호 발송 API 호출로 교체
         */
        emailVerified = false;

        showMessage(
            emailMessage,
            "인증번호를 발송했습니다. 이메일을 확인해주세요.",
            true
        );

        verificationCode?.focus();
    }

    sendCodeButton?.addEventListener("click", sendVerificationCode);
    resendCodeButton?.addEventListener("click", sendVerificationCode);

    confirmCodeButton?.addEventListener("click", function () {
        const code = verificationCode?.value.trim() ?? "";

        if (!/^\d{6}$/.test(code)) {
            emailVerified = false;

            showMessage(
                emailMessage,
                "인증번호 6자리를 입력해주세요."
            );

            verificationCode?.focus();
            return;
        }

        /*
         * 추후 서버 인증번호 확인 API 호출로 교체
         */
        emailVerified = true;

        showMessage(
            emailMessage,
            "이메일 인증이 완료되었습니다.",
            true
        );
    });

    signupCancelLink?.addEventListener("click", function (event) {
        event.preventDefault();

        const previousUrl = signupCancelLink.getAttribute("href");

        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "회원가입을 취소할까요?",
            message:
                "지금까지 입력한 내용은 저장되지 않습니다.\n약관 동의 화면으로 돌아갈까요?",
            leftText: "계속 작성",
            rightText: "작성 취소",
            leftClass: "btn-outline",
            rightClass: "btn-danger",

            onLeft: function () {
                // 모달만 닫고 현재 페이지 유지
            },

            onRight: function () {
                window.location.href = previousUrl;
            }
        });
    });

    form.addEventListener("submit", function (event) {
        if (!idChecked) {
            event.preventDefault();

            showMessage(
                idMessage,
                "아이디 중복확인을 진행해주세요."
            );

            idInput?.focus();
            return;
        }

        if (!validatePasswordMatch()) {
            event.preventDefault();
            passwordConfirm?.focus();
            return;
        }

        if (!emailVerified) {
            event.preventDefault();

            showMessage(
                emailMessage,
                "이메일 인증을 완료해주세요."
            );

            emailInput?.focus();
        }
    });
});