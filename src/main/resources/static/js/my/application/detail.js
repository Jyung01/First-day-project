document.addEventListener("DOMContentLoaded", () => {
    const cancelButton = document.querySelector("[data-application-cancel]:not(:disabled)");

    cancelButton?.addEventListener("click", () => {
        const shouldCancel = window.confirm("지원을 취소할까요?\n취소한 지원은 되돌릴 수 없습니다.");
        if (!shouldCancel) return;

        cancelButton.disabled = true;
        cancelButton.textContent = "지원 취소됨";
    });
});
