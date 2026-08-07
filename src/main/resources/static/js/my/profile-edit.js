document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-profile-edit-form]");

    if (!form) {
        return;
    }

    const imageInput = form.querySelector("[data-profile-image-input]");
    const imagePreview = form.querySelector("[data-profile-image-preview]");
    const imageMessage = form.querySelector("[data-profile-image-message]");

    const phoneInput = form.querySelector("[data-profile-phone]");
    const withdrawButton = form.querySelector("[data-withdraw]");

    /* =====================================================
       메시지
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

    /* =====================================================
       프로필 이미지 미리보기
    ===================================================== */

    imageInput?.addEventListener("change", function () {
        const file = imageInput.files?.[0];

        clearMessage(imageMessage);

        if (!file) {
            return;
        }

        const allowedTypes = ["image/jpeg", "image/png"];

        if (!allowedTypes.includes(file.type)) {
            imageInput.value = "";

            showMessage(imageMessage, "JPG 또는 PNG 이미지만 등록할 수 있습니다.");

            return;
        }

        const maximumSize = 5 * 1024 * 1024;

        if (file.size > maximumSize) {
            imageInput.value = "";

            showMessage(imageMessage, "프로필 이미지는 5MB 이하로 등록해주세요.");

            return;
        }

        const reader = new FileReader();

        reader.addEventListener("load", function () {
            if (!imagePreview) {
                return;
            }

            imagePreview.innerHTML = "";

            const image = document.createElement("img");

            image.src = reader.result;
            image.alt = "변경할 프로필 이미지 미리보기";
            image.dataset.profileImage = "";

            imagePreview.appendChild(image);

            showMessage(imageMessage, "선택한 이미지가 미리보기에 반영되었습니다.", true);
        });

        reader.readAsDataURL(file);
    });

    /* =====================================================
       휴대전화 자동 하이픈
    ===================================================== */

    phoneInput?.addEventListener("input", function () {
        const number = phoneInput.value.replace(/[^0-9]/g, "").slice(0, 11);

        if (number.length <= 3) {
            phoneInput.value = number;
            return;
        }

        if (number.length <= 7) {
            phoneInput.value = number.slice(0, 3) + "-" + number.slice(3);

            return;
        }

        phoneInput.value = number.slice(0, 3) + "-" + number.slice(3, 7) + "-" + number.slice(7);
    });

    /* =====================================================
       회원 탈퇴 확인 모달
    ===================================================== */

    const formMessage = form.querySelector("[data-profile-form-message]");

    withdrawButton?.addEventListener("click", function () {
        clearMessage(formMessage);

        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "회원 탈퇴를 진행할까요?",
            message:
                "탈퇴하면 작성한 이력서와 지원 정보 등 일부 데이터를 더 이상 이용할 수 없습니다.\n탈퇴 처리는 되돌릴 수 없습니다.\n계속하려면 현재 비밀번호를 입력해주세요.",
            extraHtml:
                '<input type="password" id="withdrawPassword" class="profile-input" placeholder="현재 비밀번호" autocomplete="current-password" />',
            leftText: "취소",
            rightText: "회원 탈퇴",
            leftClass: "btn-outline",
            rightClass: "btn-danger",
            closeOnOverlay: false,

            onRight: async function () {
                const passwordInput = document.getElementById("withdrawPassword");
                const currentPassword = passwordInput?.value ?? "";

                if (!currentPassword.trim()) {
                    showMessage(formMessage, "현재 비밀번호를 입력해주세요.");
                    return;
                }

                try {
                    const response = await fetch("/my/withdraw", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json",
                        },
                        body: JSON.stringify({ currentPassword: currentPassword }),
                    });

                    const result = await readJsonResponse(response);

                    if (!response.ok || !result.success) {
                        showMessage(formMessage, result.message || "회원 탈퇴에 실패했습니다.");
                        return;
                    }

                    window.location.href = result.redirectUrl || "/auth/login";
                } catch (error) {
                    showMessage(formMessage, "잠시 후 다시 시도해주세요.");
                }
            },
        });
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
       폼 제출
    ===================================================== */

    form.addEventListener("submit", function (event) {
        /*
         * 현재는 HTML 기본 검증을 사용한다.
         * 실제 구현 시 Spring Validation 결과와 연결한다.
         */

        if (!form.checkValidity()) {
            event.preventDefault();
            form.reportValidity();
        }
    });
});
