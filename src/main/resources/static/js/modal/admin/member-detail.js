document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-member-detail-modal]");

    if (!modal) {
        return;
    }

    const openButtons = document.querySelectorAll("[data-member-detail-open]");

    const closeButtons = modal.querySelectorAll("[data-member-detail-close]");

    const suspendButton = modal.querySelector("[data-member-suspend]");

    const number = modal.querySelector("[data-member-number]");

    const type = modal.querySelector("[data-member-type]");

    const name = modal.querySelector("[data-member-name]");

    const email = modal.querySelector("[data-member-email]");

    const createdAt = modal.querySelector("[data-member-created-at]");

    const lastLogin = modal.querySelector("[data-member-last-login]");

    const status = modal.querySelector("[data-member-status]");

    let selectedMemberId = null;
    let selectedMemberStatus = "ACTIVE";
    let previousFocus = null;

    function setText(element, value, fallback = "-") {
        if (!element) {
            return;
        }

        element.textContent = value || fallback;
    }

    function renderStatus(memberStatus) {
        selectedMemberStatus = memberStatus === "SUSPENDED" ? "SUSPENDED" : "ACTIVE";

        if (!status || !suspendButton) {
            return;
        }

        status.classList.remove("is-active", "is-suspended");

        suspendButton.classList.remove("admin-modal-button--danger", "admin-modal-button--primary");

        if (selectedMemberStatus === "SUSPENDED") {
            status.classList.add("is-suspended");
            status.innerHTML = '<span aria-hidden="true">●</span> 이용정지 중';

            suspendButton.textContent = "정지 해제";
            suspendButton.classList.add("admin-modal-button--primary");

            return;
        }

        status.classList.add("is-active");
        status.innerHTML = '<span aria-hidden="true">●</span> 정상 이용 중';

        suspendButton.textContent = "이용정지";
        suspendButton.classList.add("admin-modal-button--danger");
    }

    function openModal(button) {
        previousFocus = document.activeElement;

        selectedMemberId = button.dataset.memberId || null;

        setText(number, button.dataset.memberNumber);

        setText(type, button.dataset.memberType, "개인회원");

        setText(name, button.dataset.memberName);

        setText(email, button.dataset.memberEmail);

        setText(createdAt, button.dataset.memberCreatedAt);

        setText(lastLogin, button.dataset.memberLastLogin);

        renderStatus(button.dataset.memberStatus);

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");

        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";

        previousFocus?.focus();
    }

    openButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            openModal(button);
        });
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    suspendButton?.addEventListener("click", function () {
        const nextStatus = selectedMemberStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE";

        /*
         * 실제 구현 시 selectedMemberId를 이용해
         * 이용정지 또는 정지 해제 API를 호출한다.
         */

        console.log({
            memberId: selectedMemberId,
            status: nextStatus,
        });

        renderStatus(nextStatus);
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });
});
