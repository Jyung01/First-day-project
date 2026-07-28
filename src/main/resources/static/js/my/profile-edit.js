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

    withdrawButton?.addEventListener("click", function () {
        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "회원 탈퇴를 진행할까요?",
            message:
                "탈퇴하면 작성한 이력서와 지원 정보 등 일부 데이터를 더 이상 이용할 수 없습니다.\n탈퇴 처리는 되돌릴 수 없습니다.",
            leftText: "취소",
            rightText: "회원 탈퇴",
            leftClass: "btn-outline",
            rightClass: "btn-danger",

            onRight: function () {
                /*
                 * 실제 구현 시 회원 탈퇴 POST API로 교체
                 *
                 * 예:
                 * fetch("/my/withdraw", {
                 *     method: "POST",
                 *     headers: {
                 *         "X-CSRF-TOKEN": csrfToken
                 *     }
                 * });
                 */
                console.log("회원 탈퇴 처리 예정");
            },
        });
    });

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
