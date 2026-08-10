document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-member-detail-modal]");

    if (!modal) {
        return;
    }

    const openButtons = document.querySelectorAll("[data-member-detail-open]");
    const closeButtons = modal.querySelectorAll("[data-member-detail-close]");
    const suspendButton = modal.querySelector("[data-member-suspend]");
    const number = modal.querySelector("[data-member-number]");
    const loginId = modal.querySelector("[data-member-login-id]");
    const name = modal.querySelector("[data-member-name]");
    const email = modal.querySelector("[data-member-email]");
    const phone = modal.querySelector("[data-member-phone]");
    const createdAt = modal.querySelector("[data-member-created-at]");
    const lastLogin = modal.querySelector("[data-member-last-login]");
    const withdrawnField = modal.querySelector("[data-member-withdrawn-field]");
    const withdrawnAt = modal.querySelector("[data-member-withdrawn-at]");
    const status = modal.querySelector("[data-member-status]");
    const guide = modal.querySelector("[data-member-guide]");

    let selectedMemberStatus = "ACTIVE";
    let selectedMemberDetailUrl = null;
    let previousFocus = null;
    let requestSequence = 0;

    function setText(element, value, fallback = "-") {
        if (element) {
            element.textContent = value || fallback;
        }
    }

    function formatDateTime(value, includeTime = false) {
        if (!value) {
            return "";
        }

        const [date, time = ""] = value.split("T");
        const formattedDate = date.replaceAll("-", ".");

        return includeTime && time
            ? `${formattedDate} ${time.slice(0, 5)}`
            : formattedDate;
    }

    function renderStatus(memberStatus) {
        selectedMemberStatus = ["ACTIVE", "SUSPENDED", "WITHDRAWN"].includes(memberStatus)
            ? memberStatus
            : "ACTIVE";

        if (!status || !suspendButton) {
            return;
        }

        status.classList.remove("is-active", "is-suspended", "is-withdrawn");
        suspendButton.classList.remove("admin-modal-button--danger", "admin-modal-button--primary");
        withdrawnField.hidden = selectedMemberStatus !== "WITHDRAWN";
        suspendButton.hidden = selectedMemberStatus === "WITHDRAWN";

        if (selectedMemberStatus === "WITHDRAWN") {
            status.classList.add("is-withdrawn");
            status.innerHTML = '<span aria-hidden="true">●</span> 탈퇴한 계정';
            guide.textContent = "탈퇴한 회원은 조회만 가능하며 계정 상태를 변경할 수 없습니다.";
            return;
        }

        if (selectedMemberStatus === "SUSPENDED") {
            status.classList.add("is-suspended");
            status.innerHTML = '<span aria-hidden="true">●</span> 이용정지 중';
            suspendButton.textContent = "정지 해제";
            suspendButton.classList.add("admin-modal-button--primary");
            guide.textContent = "정지를 해제하면 회원이 다시 로그인하여 서비스를 이용할 수 있습니다.";
            return;
        }

        status.classList.add("is-active");
        status.innerHTML = '<span aria-hidden="true">●</span> 정상 이용 중';
        suspendButton.textContent = "이용정지";
        suspendButton.classList.add("admin-modal-button--danger");
        guide.textContent = "이용정지하면 로그인이 제한되며 기존 지원·후기 이력은 보존됩니다.";
    }

    function showModal() {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }

    function renderLoading() {
        [number, loginId, name, email, phone, createdAt, lastLogin, withdrawnAt]
            .forEach(function (element) {
                setText(element, "불러오는 중...");
            });
        withdrawnField.hidden = true;
        suspendButton.hidden = true;
        status.classList.remove("is-active", "is-suspended", "is-withdrawn");
        status.textContent = "조회 중";
        guide.textContent = "회원 정보를 불러오고 있습니다.";
        modal.setAttribute("aria-busy", "true");
    }

    function renderMember(member) {
        setText(number, member.userId);
        setText(loginId, member.loginId);
        setText(name, member.name);
        setText(email, member.email);
        setText(phone, member.phone);
        setText(createdAt, formatDateTime(member.createdAt));
        setText(lastLogin, formatDateTime(member.lastLoginAt, true));
        setText(withdrawnAt, formatDateTime(member.withdrawnAt));
        renderStatus(member.statusCode);
    }

    function renderLoadError() {
        [number, loginId, name, email, phone, createdAt, lastLogin, withdrawnAt]
            .forEach(function (element) {
                setText(element, "-");
            });
        withdrawnField.hidden = true;
        suspendButton.hidden = true;
        status.classList.remove("is-active", "is-suspended", "is-withdrawn");
        status.textContent = "조회 실패";
        guide.textContent = "회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
    }

    async function openModal(button) {
        const detailUrl = button.dataset.memberDetailUrl;
        const currentRequest = ++requestSequence;

        previousFocus = document.activeElement;
        selectedMemberDetailUrl = detailUrl;
        renderLoading();
        showModal();

        try {
            const response = await fetch(detailUrl, {
                headers: { Accept: "application/json" },
            });

            if (!response.ok) {
                throw new Error(`회원 상세 조회 실패: ${response.status}`);
            }

            const member = await response.json();
            if (currentRequest === requestSequence) {
                renderMember(member);
            }
        } catch (error) {
            if (currentRequest === requestSequence) {
                renderLoadError();
                console.error(error);
            }
        } finally {
            if (currentRequest === requestSequence) {
                modal.removeAttribute("aria-busy");
            }
        }
    }

    function closeModal() {
        requestSequence += 1;
        selectedMemberDetailUrl = null;
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        modal.removeAttribute("aria-busy");
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

    suspendButton?.addEventListener("click", async function () {
        if (!selectedMemberDetailUrl
                || !["ACTIVE", "SUSPENDED"].includes(selectedMemberStatus)) {
            return;
        }

        const suspending = selectedMemberStatus === "ACTIVE";
        const action = suspending ? "suspend" : "unsuspend";
        const confirmationMessage = suspending
            ? "이 회원을 이용정지하시겠습니까?\n현재 로그인 중이라면 다음 요청부터 로그아웃됩니다."
            : "이 회원의 이용정지를 해제하시겠습니까?";

        if (!window.confirm(confirmationMessage)) {
            return;
        }

        suspendButton.disabled = true;
        suspendButton.textContent = "처리 중...";

        try {
            const response = await fetch(`${selectedMemberDetailUrl}/${action}`, {
                method: "POST",
                headers: { Accept: "application/json" },
            });

            if (!response.ok) {
                throw new Error(`회원 상태 변경 실패: ${response.status}`);
            }

            window.location.reload();
        } catch (error) {
            console.error(error);
            window.alert("회원 상태를 변경하지 못했습니다. 새로고침 후 다시 시도해 주세요.");
            suspendButton.disabled = false;
            renderStatus(selectedMemberStatus);
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });
});
