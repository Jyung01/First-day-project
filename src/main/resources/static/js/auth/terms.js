document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-terms-form]");

    if (!form) {
        return;
    }

    const checkAll = form.querySelector("[data-check-all]");
    const termCheckboxes = Array.from(form.querySelectorAll("[data-term-checkbox]"));
    const requiredCheckboxes = termCheckboxes.filter((checkbox) => checkbox.dataset.required === "true");
    const errorMessage = form.querySelector("[data-terms-error]");

    const modal = document.querySelector("[data-terms-modal]");
    const modalTitle = modal?.querySelector("[data-modal-title]");
    const modalContent = modal?.querySelector("[data-modal-content]");
    const modalCloseButtons = modal?.querySelectorAll("[data-modal-close]");
    const modalOpenButtons = document.querySelectorAll("[data-modal-type]");

    const modalData = {
        "member-service": {
            title: "개인회원 서비스 이용약관",
            content:
                "개인회원 서비스 이용과 관련된 권리, 의무 및 책임사항을 안내하는 약관입니다.\n\n실제 구현 단계에서는 관리자 정책 관리에 저장된 약관 내용을 출력하면 됩니다.",
        },
        "company-service": {
            title: "기업회원 서비스 이용약관",
            content:
                "기업회원 서비스 이용과 관련된 권리, 의무 및 책임사항을 안내하는 약관입니다.\n\n실제 구현 단계에서는 관리자 정책 관리에 저장된 약관 내용을 출력하면 됩니다.",
        },
        privacy: {
            title: "개인정보 수집·이용 동의",
            content: "회원가입과 서비스 제공을 위해 필요한 개인정보의 수집 항목, 이용 목적 및 보유 기간을 안내합니다.",
        },
        marketing: {
            title: "마케팅 정보 수신 동의",
            content: "이벤트, 프로모션 및 서비스 안내 정보 수신에 관한 선택 동의 내용입니다.",
        },
        "recruit-email": {
            title: "채용정보 이메일 수신 동의",
            content: "맞춤 채용공고와 채용 관련 정보를 이메일로 수신하는 것에 관한 선택 동의 내용입니다.",
        },
        "company-info": {
            title: "기업정보 공개 동의",
            content: "기업회원이 등록한 기업정보를 첫출근 서비스 내에 공개하는 것에 관한 동의 내용입니다.",
        },
    };

    function updateCheckAll() {
        const allChecked = termCheckboxes.length > 0 && termCheckboxes.every((checkbox) => checkbox.checked);

        checkAll.checked = allChecked;
        checkAll.indeterminate = !allChecked && termCheckboxes.some((checkbox) => checkbox.checked);
    }

    function hideError() {
        errorMessage?.classList.remove("is-visible");
    }

    checkAll?.addEventListener("change", function () {
        termCheckboxes.forEach(function (checkbox) {
            checkbox.checked = checkAll.checked;
        });

        checkAll.indeterminate = false;
        hideError();
    });

    termCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            updateCheckAll();

            if (requiredCheckboxes.every((requiredCheckbox) => requiredCheckbox.checked)) {
                hideError();
            }
        });
    });

    form.addEventListener("submit", function (event) {
        const requiredAccepted = requiredCheckboxes.every((checkbox) => checkbox.checked);

        if (!requiredAccepted) {
            event.preventDefault();
            errorMessage?.classList.add("is-visible");

            const firstUnchecked = requiredCheckboxes.find((checkbox) => !checkbox.checked);

            firstUnchecked?.focus();
        }
    });

    function openModal(type) {
        if (!modal) {
            return;
        }

        const data = modalData[type];

        if (!data) {
            return;
        }

        modalTitle.textContent = data.title;
        modalContent.textContent = data.content;

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        if (!modal) {
            return;
        }

        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";
    }

    modalOpenButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            openModal(button.dataset.modalType);
        });
    });

    modalCloseButtons?.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal?.classList.contains("is-open")) {
            closeModal();
        }
    });
});
