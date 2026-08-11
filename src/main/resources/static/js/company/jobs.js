document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".bookmark-btn").forEach((button) => {
        button.addEventListener("click", async (event) => {
            event.preventDefault();
            event.stopPropagation();

            if (button.dataset.loggedIn !== "true") {
                showConfirmModal({
                    iconClass: "info",
                    title: "로그인이 필요합니다",
                    message: "관심공고는 개인회원 로그인 후 등록할 수 있습니다.",
                    leftText: "취소",
                    rightText: "로그인",
                    onRight: () => {
                        const returnUrl = window.location.pathname + window.location.search;
                        window.location.href = `/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
                    }
                });
                return;
            }

            button.disabled = true;

            try {
                const response = await fetch(`/api/jobs/${button.dataset.jobPostingId}/bookmark`, {
                    method: "POST"
                });

                if (!response.ok) {
                    const result = await response.json().catch(() => ({}));
                    throw new Error(result.message || "관심공고를 처리하지 못했습니다.");
                }

                const result = await response.json();
                renderBookmark(button, result.bookmarked);
            } catch (error) {
                showConfirmModal({
                    iconClass: "danger",
                    title: "처리할 수 없습니다",
                    message: error.message,
                    leftVisible: false,
                    rightText: "확인"
                });
            } finally {
                button.disabled = false;
            }
        });
    });
});

function renderBookmark(button, bookmarked) {
    const icon = button.querySelector("i");

    button.classList.toggle("active", bookmarked);
    button.setAttribute("aria-pressed", String(bookmarked));
    button.setAttribute("aria-label", bookmarked ? "관심 공고 해제" : "관심 공고 등록");
    icon.classList.toggle("fa-solid", bookmarked);
    icon.classList.toggle("fa-regular", !bookmarked);
}
