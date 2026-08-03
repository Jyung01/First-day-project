document.addEventListener("DOMContentLoaded", () => {
  const cancelButton = document.querySelector(
    "[data-application-cancel]:not(:disabled)",
  );

  cancelButton?.addEventListener("click", () => {
    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "지원을 취소할까요?",
      message:
        "취소 후에는 되돌릴 수 없습니다.\n지원 내역에는 지원 취소 상태로 계속 표시됩니다.",
      leftText: "계속 지원",
      rightText: "지원 취소",
      rightClass: "btn-danger",
      onRight: () => {
        cancelButton.disabled = true;
        cancelButton.textContent = "지원 취소됨";
      },
    });
  });
});
