document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-password-change-modal]");

    const openButtons = document.querySelectorAll("[data-password-change-open]");

    if (!modal || openButtons.length === 0) {
        return;
    }

    const form = modal.querySelector("[data-password-change-form]");

    const closeButtons = modal.querySelectorAll("[data-password-change-close]");

    const currentPassword = modal.querySelector("[data-current-password]");

    const newPassword = modal.querySelector("[data-new-password]");

    const newPasswordConfirm = modal.querySelector("[data-new-password-confirm]");

    const currentPasswordMessage = modal.querySelector("[data-current-password-message]");

    const newPasswordMessage = modal.querySelector("[data-new-password-message]");

    const formMessage = modal.querySelector("[data-password-change-form-message]");

    const passwordToggleButtons = modal.querySelectorAll("[data-password-toggle]");

    let previouslyFocusedElement = null;

    /* =====================================================
       메시지 처리
    ===================================================== */

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

    function clearInputState(input) {
        input?.classList.remove("is-error", "is-success");
    }

    function showInputError(input) {
        input?.classList.remove("is-success");
        input?.classList.add("is-error");
    }

    function showInputSuccess(input) {
        input?.classList.remove("is-error");
        input?.classList.add("is-success");
    }

    /* =====================================================
       모달 열기 / 닫기
    ===================================================== */

    function resetForm() {
        form?.reset();

        clearMessage(currentPasswordMessage);
        clearMessage(newPasswordMessage);
        clearMessage(formMessage);

        clearInputState(currentPassword);
        clearInputState(newPassword);
        clearInputState(newPasswordConfirm);

        passwordToggleButtons.forEach(function (button) {
            const targetId = button.dataset.target;
            const targetInput = document.getElementById(targetId);

            if (targetInput) {
                targetInput.type = "password";
            }

            button.textContent = "보기";
        });
    }

    function openModal() {
        previouslyFocusedElement = document.activeElement;

        resetForm();

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");

        document.body.style.overflow = "hidden";

        window.setTimeout(function () {
            currentPassword?.focus();
        }, 0);
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";

        resetForm();

        previouslyFocusedElement?.focus();
    }

    openButtons.forEach(function (button) {
        button.addEventListener("click", openModal);
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    /* =====================================================
       비밀번호 보기 / 숨기기
    ===================================================== */

    passwordToggleButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const targetId = button.dataset.target;
            const targetInput = document.getElementById(targetId);

            if (!targetInput) {
                return;
            }

            const showingPassword = targetInput.type === "password";

            targetInput.type = showingPassword ? "text" : "password";

            button.textContent = showingPassword ? "숨기기" : "보기";

            button.setAttribute("aria-label", showingPassword ? "비밀번호 숨기기" : "비밀번호 표시");
        });
    });

    /* =====================================================
       새 비밀번호 검증
    ===================================================== */

    function isValidPassword(password) {
        const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

        return passwordPattern.test(password);
    }

    function validateNewPassword() {
        if (!newPassword || !newPasswordConfirm) {
            return false;
        }

        const password = newPassword.value;
        const passwordConfirm = newPasswordConfirm.value;

        clearMessage(newPasswordMessage);
        clearInputState(newPassword);
        clearInputState(newPasswordConfirm);

        if (!password) {
            showMessage(newPasswordMessage, "새 비밀번호를 입력해주세요.");

            showInputError(newPassword);

            return false;
        }

        if (!isValidPassword(password)) {
            showMessage(newPasswordMessage, "영문·숫자·특수문자를 포함해 8자 이상 입력해주세요.");

            showInputError(newPassword);

            return false;
        }

        if (!passwordConfirm) {
            showMessage(newPasswordMessage, "새 비밀번호 확인을 입력해주세요.");

            showInputError(newPasswordConfirm);

            return false;
        }

        if (password !== passwordConfirm) {
            showMessage(newPasswordMessage, "새 비밀번호가 일치하지 않습니다.");

            showInputError(newPasswordConfirm);

            return false;
        }

        if (currentPassword?.value && currentPassword.value === password) {
            showMessage(newPasswordMessage, "현재 비밀번호와 다른 비밀번호를 입력해주세요.");

            showInputError(newPassword);

            return false;
        }

        showMessage(newPasswordMessage, "새 비밀번호가 일치합니다.", true);

        showInputSuccess(newPassword);
        showInputSuccess(newPasswordConfirm);

        return true;
    }

    currentPassword?.addEventListener("input", function () {
        clearMessage(currentPasswordMessage);
        clearMessage(formMessage);
        clearInputState(currentPassword);
    });

    newPassword?.addEventListener("input", function () {
        clearMessage(formMessage);

        if (newPasswordConfirm?.value) {
            validateNewPassword();
        } else {
            clearMessage(newPasswordMessage);
            clearInputState(newPassword);
        }
    });

    newPasswordConfirm?.addEventListener("input", validateNewPassword);

    /* =====================================================
       폼 제출
    ===================================================== */

    form?.addEventListener("submit", async function (event) {
        event.preventDefault();

        clearMessage(currentPasswordMessage);
        clearMessage(formMessage);
        clearInputState(currentPassword);

        if (!currentPassword?.value.trim()) {
            showMessage(currentPasswordMessage, "현재 비밀번호를 입력해주세요.");

            showInputError(currentPassword);
            currentPassword?.focus();

            return;
        }

        if (!validateNewPassword()) {
            newPasswordConfirm?.focus();
            return;
        }

        const requestData = {
            currentPassword: currentPassword.value,
            newPassword: newPassword.value,
            newPasswordConfirm: newPasswordConfirm.value,
        };

        try {
            const response = await fetch("/my/password-change", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(requestData),
            });

            const result = await readJsonResponse(response);

            if (!response.ok || !result.success) {
                showMessage(
                    currentPasswordMessage,
                    result.message || "현재 비밀번호가 일치하지 않습니다."
                );

                showInputError(currentPassword);
                currentPassword.focus();

                return;
            }

            closeModal();

            if (typeof showConfirmModal === "function") {
                showConfirmModal({
                    iconClass: "success",
                    iconHtml: "✓",
                    title: "비밀번호가 변경되었습니다",
                    message: "다음 로그인부터 새로운 비밀번호를 사용해주세요.",
                    // 안내만 하는 모달이라 왼쪽 버튼을 숨긴다.
                    // leftText만 비우면 버튼은 그대로 남아 빈 입력창처럼 보인다.
                    leftVisible: false,
                    rightText: "확인",
                    rightClass: "btn-primary",
                });
            }
        } catch (error) {
            showMessage(formMessage, "잠시 후 다시 시도해주세요.");
        }
    });

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

    /* =====================================================
       ESC 키 닫기
    ===================================================== */

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });
});
