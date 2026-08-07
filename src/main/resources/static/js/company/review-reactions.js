document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".help-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            if (button.disabled) return;
            button.disabled = true;

            const params = new URLSearchParams({
                reviewType: button.dataset.reviewType,
                reviewId: button.dataset.reviewId
            });

            try {
                const response = await fetch("/company/review/help", {
                    method: "POST",
                    headers: {"Content-Type": "application/x-www-form-urlencoded"},
                    body: params
                });
                const result = await response.json();

                if (!response.ok) {
                    alert(result.message || "도움돼요 처리에 실패했습니다.");
                    if (response.status === 401) {
                        location.href = "/auth/login";
                    }
                    return;
                }

                button.querySelector(".help-count").textContent = result.helpCount;
                button.classList.toggle("active", result.helpful);
            } catch (error) {
                alert("도움돼요 처리 중 오류가 발생했습니다.");
            } finally {
                button.disabled = false;
            }
        });
    });
});
