document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".review-btn").forEach((button) => {
        button.addEventListener("click", () => {
            if (button.dataset.loggedIn === "true") {
                window.location.href = button.dataset.reviewUrl;
                return;
            }

            showConfirmModal({
                iconClass: "info",
                title: "로그인이 필요합니다",
                message: "기업 후기는 로그인 후 확인할 수 있습니다.",
                leftText: "취소",
                rightText: "로그인",
                onRight: () => {
                    const returnUrl = window.location.pathname + window.location.search;
                    window.location.href = `/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
                }
            });
        });
    });

    document.querySelectorAll(".wish-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            if (button.disabled) return;
            button.disabled = true;
            try {
                const response = await fetch(`/company/wish/${button.dataset.companyId}`, { method: "POST" });
                const result = await response.json().catch(() => ({}));
                if (response.status === 401) {
                    showConfirmModal({
                        iconClass: "info",
                        title: "로그인이 필요합니다",
                        message: "관심기업은 로그인 후 등록할 수 있습니다.",
                        leftText: "취소",
                        rightText: "로그인",
                        onRight: () => {
                            const returnUrl = location.pathname + location.search;
                            location.href = `/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
                        }
                    });
                    return;
                }
                if (!response.ok || !result.success) {
                    throw new Error(result.message || "관심기업 처리에 실패했습니다.");
                }
                button.classList.toggle("active", result.wished);
                const icon = button.querySelector("i");
                icon.classList.toggle("fa-solid", result.wished);
                icon.classList.toggle("fa-regular", !result.wished);
                button.setAttribute("aria-pressed", String(result.wished));
            } catch (error) {
                showConfirmModal({
                    iconClass: "danger",
                    title: "처리할 수 없습니다",
                    message: error.message || "관심기업을 처리하지 못했습니다.",
                    leftVisible: false,
                    rightText: "확인"
                });
            } finally {
                button.disabled = false;
            }
        });
    });
});
