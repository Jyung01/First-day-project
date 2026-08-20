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

    const businessNumberInput = form.querySelector("[data-business-number-input]");
    const businessNumberCheckButton = form.querySelector("[data-business-number-check]");
    const businessNumberMessage = form.querySelector("[data-business-number-message]");

    const emailInput = form.querySelector("[data-email-input]");
    const sendCodeButton = form.querySelector("[data-send-code]");
    const resendCodeButton = form.querySelector("[data-resend-code]");
    const verificationCode = form.querySelector("[data-verification-code]");
    const confirmCodeButton = form.querySelector("[data-confirm-code]");
    const emailMessage = form.querySelector("[data-email-message]");
    const signupCancelLink = document.querySelector("[data-signup-cancel]");


    let idChecked = form.dataset.serverIdChecked === "true";
    let emailVerified = form.dataset.serverEmailVerified === "true";
    let businessNumberChecked = !businessNumberInput
        || form.dataset.serverBusinessNumberChecked === "true";

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

    /*
     * 인증번호 입력칸과 확인·재발송 버튼은 인증번호가 실제로 발송된 뒤에만 쓸 수 있다.
     * 발송 전에 열어두면 넣을 수 있는 코드가 없는데도 입력이 가능해 혼란스럽고,
     * "먼저 인증번호를 요청해주세요" 같은 서버 응답을 받고 나서야 알게 된다.
     *
     * 상태가 여러 곳(발송 성공, 요청 진행 중, 이메일 변경)에서 바뀌므로
     * 값은 아래 두 플래그로만 두고 화면 반영은 이 함수 하나가 담당한다.
     */
    let codeSent = false;
    let emailRequestInFlight = false;

    function syncEmailControls() {
        /*
         * 인증번호 받기는 아직 인증하지 않았을 때만 필요하다.
         * 요청이 도는 동안에는 중복 요청을 막는다.
         */
        if (sendCodeButton) {
            sendCodeButton.disabled = emailRequestInFlight || emailVerified;
        }

        /*
         * 인증번호 입력·재전송·확인은 "발송됐고 아직 인증 전"일 때만 쓸 수 있다.
         * 인증이 끝나면 더 입력할 것이 없으므로 잠근다.
         * 이메일을 고치면 emailVerified·codeSent가 모두 풀려 처음 상태로 돌아간다.
         */
        const codeControlsEnabled =
            codeSent && !emailRequestInFlight && !emailVerified;

        [resendCodeButton, confirmCodeButton, verificationCode]
            .filter(Boolean)
            .forEach(function (element) {
                element.disabled = !codeControlsEnabled;
            });
    }

    function setEmailRequestInFlight(inFlight) {
        emailRequestInFlight = inFlight;
        syncEmailControls();
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

    /*
     * 첫 진입 시점에는 아직 발송된 인증번호가 없으므로 잠근 상태로 시작한다.
     * 검증 실패로 폼이 다시 그려진 경우도 마찬가지다. 세션의 인증번호는 남아 있을 수 있지만,
     * 사용자는 이미 인증을 마쳤거나(위 메시지) 다시 받아야 하는 상태다.
     */
    syncEmailControls();
    if (businessNumberInput && businessNumberChecked) {
        showMessage(
            businessNumberMessage,
            "사용 가능한 사업자등록번호입니다.",
            true
        );
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

    businessNumberInput?.addEventListener("input", function () {
        const number = businessNumberInput.value
            .replace(/[^0-9]/g, "")
            .slice(0, 10);

        businessNumberChecked = false;
        clearMessage(businessNumberMessage);

        if (number.length <= 3) {
            businessNumberInput.value = number;
            return;
        }

        if (number.length <= 5) {
            businessNumberInput.value =
                number.slice(0, 3) + "-" + number.slice(3);
            return;
        }

        businessNumberInput.value =
            number.slice(0, 3) +
            "-" +
            number.slice(3, 5) +
            "-" +
            number.slice(5);
    });

    businessNumberCheckButton?.addEventListener("click", async function () {
        const businessNumber = businessNumberInput?.value.trim() ?? "";

        if (!/^\d{3}-\d{2}-\d{5}$/.test(businessNumber)) {
            businessNumberChecked = false;
            showMessage(
                businessNumberMessage,
                "사업자등록번호 10자리를 정확히 입력해주세요."
            );
            businessNumberInput?.focus();
            return;
        }

        businessNumberChecked = false;
        clearMessage(businessNumberMessage);
        businessNumberCheckButton.disabled = true;

        try {
            const response = await fetch(
                "/api/auth/business-number-availability?businessNumber="
                    + encodeURIComponent(businessNumber)
            );
            const data = await response.json();

            if (businessNumberInput?.value.trim() !== businessNumber) {
                showMessage(
                    businessNumberMessage,
                    "사업자등록번호가 변경되었습니다. 중복확인을 다시 진행해주세요."
                );
                return;
            }

            businessNumberChecked = response.ok && data.available === true;
            showMessage(
                businessNumberMessage,
                data.message,
                businessNumberChecked
            );
        } catch (error) {
            showMessage(
                businessNumberMessage,
                "사업자등록번호 중복확인을 처리하지 못했습니다."
            );
        } finally {
            businessNumberCheckButton.disabled = false;
        }
    });

    emailInput?.addEventListener("input", function () {
        emailVerified = false;
        clearMessage(emailMessage);

        /*
         * 이메일이 바뀌면 앞서 받은 인증번호는 다른 주소의 것이라 쓸 수 없다.
         * 서버도 "인증번호를 요청한 이메일과 일치하지 않습니다"로 거절하므로,
         * 입력칸을 비우고 다시 잠가 인증번호부터 받도록 되돌린다.
         */
        codeSent = false;

        if (verificationCode) {
            verificationCode.value = "";
        }

        syncEmailControls();
    });

    async function sendVerificationCode() {
        if (!emailInput?.checkValidity()) {
            emailInput?.reportValidity();
            return;
        }

        const requestedEmail = emailInput.value.trim();
        emailVerified = false;
        clearMessage(emailMessage);
        setEmailRequestInFlight(true);

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

            // 발송이 성공한 지금부터 인증번호 입력칸과 확인·재발송 버튼을 쓸 수 있다.
            codeSent = true;
            syncEmailControls();

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
            setEmailRequestInFlight(false);
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
        setEmailRequestInFlight(true);

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

            /*
             * 인증 완료. 아래 finally의 setEmailRequestInFlight(false)가
             * syncEmailControls를 부르면서 인증번호 관련 입력이 모두 잠긴다.
             */
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
            setEmailRequestInFlight(false);
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

        if (!businessNumberChecked) {
            event.preventDefault();

            showMessage(
                businessNumberMessage,
                "사업자등록번호 중복확인을 진행해주세요."
            );

            businessNumberInput?.focus();
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
