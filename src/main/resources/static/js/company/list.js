document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".wish-btn").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            e.stopPropagation();

            if (btn.disabled) return;
            btn.disabled = true;

            const companyId = btn.dataset.companyId;

            try {
                const response = await fetch(`/company/wish/${companyId}`, {
                    method: "POST"
                });
                const data = await response.json().catch(() => ({}));

                if (response.status === 401) {
                    showWishLoginModal();
                    return;
                }

                if (!response.ok || !data.success) {
                    throw new Error(data.message || "관심기업을 처리하지 못했습니다.");
                }

                renderWish(btn, data.wished);
            } catch (error) {
                showWishErrorModal(error.message);
            } finally {
                btn.disabled = false;
            }
        });
    });
});

function renderWish(button, wished) {
    const icon = button.querySelector("i");
    button.classList.toggle("active", wished);
    button.setAttribute("aria-pressed", String(wished));
    icon?.classList.toggle("fa-solid", wished);
    icon?.classList.toggle("fa-regular", !wished);
}

function showWishLoginModal() {
    showConfirmModal({
        iconClass: "info",
        title: "로그인이 필요합니다",
        message: "관심기업은 로그인 후 등록할 수 있습니다.",
        leftText: "취소",
        rightText: "로그인",
        onRight: () => {
            const returnUrl = window.location.pathname + window.location.search;
            window.location.href = `/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
        }
    });
}

function showWishErrorModal(message) {
    showConfirmModal({
        iconClass: "danger",
        title: "처리할 수 없습니다",
        message: message || "관심기업을 처리하지 못했습니다.",
        leftVisible: false,
        rightText: "확인"
    });
}
