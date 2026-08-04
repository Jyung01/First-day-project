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


    let idChecked = form.dataset.serverIdChecked === "true";
    let emailVerified = form.dataset.serverEmailVerified === "true";

    async function requestEmailVerification(url, body) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {
            "Content-Type": "application/json",
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(url, {
            method: "POST",
            headers,
            body: JSON.stringify(body),
        });
        const data = await response.json().catch(function () {
            return {};
        });

        if (!response.ok) {
            throw new Error(
                data.message ?? data.detail ?? "요청을 처리하지 못했습니다."
            );
        }

        return data;
    }

    function setEmailButtonsDisabled(disabled) {
        [sendCodeButton, resendCodeButton, confirmCodeButton]
            .filter(Boolean)
            .forEach(function (button) {
                button.disabled = disabled;
            });
    }

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

        const policyValid = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d\s])\S{8,64}$/.test(
            password.value
        );

        if (!policyValid) {
            showMessage(
                passwordMessage,
                "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
            return false;
        }

        if (!passwordConfirm.value) {
            showMessage(passwordMessage, "사용 가능한 비밀번호입니다.", true);
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

    if (idChecked) {
        showMessage(idMessage, "사용 가능한 아이디입니다.", true);
    }
    if (emailVerified) {
        showMessage(emailMessage, "이메일 인증이 완료되었습니다.", true);
    }

    idInput?.addEventListener("input", function () {
        idChecked = false;
        clearMessage(idMessage);
    });

    idCheckButton?.addEventListener("click", async function () {
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

        idChecked = false;
        clearMessage(idMessage);
        idCheckButton.disabled = true;

        try {
            const response = await fetch(
                "/api/auth/login-id-availability?loginId="
                    + encodeURIComponent(memberId)
            );
            const data = await response.json();

            if (idInput?.value.trim() !== memberId) {
                showMessage(
                    idMessage,
                    "아이디가 변경되었습니다. 중복확인을 다시 진행해주세요."
                );
                return;
            }

            idChecked = response.ok && data.available === true;
            showMessage(idMessage, data.message, idChecked);
        } catch (error) {
            showMessage(
                idMessage,
                "아이디 중복확인을 처리하지 못했습니다."
            );
        } finally {
            idCheckButton.disabled = false;
        }
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

    async function sendVerificationCode() {
        if (!emailInput?.checkValidity()) {
            emailInput?.reportValidity();
            return;
        }

        const requestedEmail = emailInput.value.trim();
        emailVerified = false;
        clearMessage(emailMessage);
        setEmailButtonsDisabled(true);

        try {
            const data = await requestEmailVerification(
                "/api/email-verifications/send",
                { email: requestedEmail }
            );

            if (emailInput.value.trim() !== requestedEmail) {
                showMessage(
                    emailMessage,
                    "이메일이 변경되었습니다. 인증번호를 다시 요청해주세요."
                );
                return;
            }

            if (verificationCode) {
                verificationCode.value = "";
            }
            showMessage(emailMessage, data.message, true);
            verificationCode?.focus();
        } catch (error) {
            showMessage(
                emailMessage,
                error instanceof Error
                    ? error.message
                    : "인증번호를 발송하지 못했습니다."
            );
        } finally {
            setEmailButtonsDisabled(false);
        }
    }

    sendCodeButton?.addEventListener("click", sendVerificationCode);
    resendCodeButton?.addEventListener("click", sendVerificationCode);

    confirmCodeButton?.addEventListener("click", async function () {
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

        const requestedEmail = emailInput?.value.trim() ?? "";
        emailVerified = false;
        clearMessage(emailMessage);
        setEmailButtonsDisabled(true);

        try {
            const data = await requestEmailVerification(
                "/api/email-verifications/verify",
                { email: requestedEmail, code }
            );

            if (emailInput?.value.trim() !== requestedEmail) {
                showMessage(
                    emailMessage,
                    "이메일이 변경되었습니다. 인증번호를 다시 요청해주세요."
                );
                return;
            }

            emailVerified = true;
            showMessage(emailMessage, data.message, true);
        } catch (error) {
            showMessage(
                emailMessage,
                error instanceof Error
                    ? error.message
                    : "이메일 인증을 확인하지 못했습니다."
            );
        } finally {
            setEmailButtonsDisabled(false);
        }
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
