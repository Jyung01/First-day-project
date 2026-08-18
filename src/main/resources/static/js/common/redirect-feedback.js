document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("redirectFeedback");
    if (!feedback || typeof showConfirmModal !== "function") return;

    showConfirmModal({
        iconClass: "warning",
        iconHtml: '<i class="fa-solid fa-exclamation"></i>',
        title: "확인할 수 없습니다",
        message: feedback.dataset.message,
        leftVisible: false,
        rightText: "확인",
        rightClass: "btn-primary"
    });
});
