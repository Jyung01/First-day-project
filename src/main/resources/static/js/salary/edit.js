document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(".salary-form");
    if (!form) return;
    form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        showConfirmModal({
            iconClass: "info",
            iconHtml: '<i class="fa-solid fa-question"></i>',
            title: "연봉정보를 수정할까요?",
            message: "변경한 내용은 익명 연봉 통계에 바로 반영됩니다.",
            leftText: "취소",
            rightText: "수정하기",
            leftClass: "btn-outline",
            rightClass: "btn-primary",
            onRight: () => form.submit()
        });
    });
});
