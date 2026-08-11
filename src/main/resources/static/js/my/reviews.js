document.addEventListener("DOMContentLoaded", () => {
    const companyList = document.querySelector(".company-review-list");
    const interviewList = document.querySelector(".interview-review-list");

    document.querySelectorAll(".review-tab").forEach((tab) => {
        tab.addEventListener("click", () => {
            const company = tab.dataset.type === "company";
            document.querySelectorAll(".review-tab").forEach(item => item.classList.toggle("active", item === tab));
            companyList.style.display = company ? "flex" : "none";
            interviewList.style.display = company ? "none" : "flex";
            history.replaceState(null, "", `/my/reviews?type=${tab.dataset.type}`);
        });
    });

    document.querySelectorAll(".btn-delete").forEach((button) => {
        button.addEventListener("click", () => showConfirmModal({
            iconClass: "danger", iconHtml: '<i class="fa-solid fa-exclamation"></i>',
            title: "후기를 삭제할까요?", message: "삭제한 후기는 목록과 공개 통계에서 제외됩니다.",
            leftText: "취소", rightText: "삭제", leftClass: "btn-outline", rightClass: "btn-danger",
            onRight: () => deleteReview(button)
        }));
    });

    const feedback = document.getElementById("reviewFeedback");
    if (feedback) showReviewResult(feedback.dataset.success === "true", feedback.dataset.message, false);
});

async function deleteReview(button) {
    try {
        const response = await fetch("/my/reviews/delete", {
            method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: new URLSearchParams({type: button.dataset.reviewType, reviewId: button.dataset.reviewId})
        });
        const result = await response.json();
        showReviewResult(response.ok, result.message || "후기 삭제에 실패했습니다.", response.ok);
    } catch (error) {
        showReviewResult(false, "후기 삭제 중 오류가 발생했습니다.", false);
    }
}

function showReviewResult(success, message, reload) {
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        iconHtml: success ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-exclamation"></i>',
        title: success ? "처리가 완료되었습니다" : "처리할 수 없습니다", message,
        leftVisible: false, rightText: "확인", rightClass: "btn-primary",
        onLeft: reload ? () => location.reload() : null,
        onRight: reload ? () => location.reload() : null
    });
}
