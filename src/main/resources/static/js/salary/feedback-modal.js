document.addEventListener("DOMContentLoaded", () => {
    const feedback = document.getElementById("salaryFeedback");
    if (!feedback) return;

    const success = feedback.dataset.type === "success";
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        iconHtml: success
            ? '<i class="fa-solid fa-check"></i>'
            : '<i class="fa-solid fa-exclamation"></i>',
        title: success ? "처리가 완료되었습니다" : "처리할 수 없습니다",
        message: feedback.dataset.message,
        leftVisible: false,
        rightText: "확인",
        rightClass: "btn-primary"
    });
});
