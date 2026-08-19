document.addEventListener("DOMContentLoaded", () => {
  const documentModal = document.querySelector("#applicationDocumentModal");
  const documentTitle = document.querySelector("#applicationDocumentTitle");
  const documentPanels = document.querySelectorAll("[data-document-panel]");

  const closeDocumentModal = () => {
    documentModal?.classList.remove("show");
    document.body.style.overflow = "";
  };

  document.querySelectorAll("[data-document-open]").forEach((button) => {
    button.addEventListener("click", () => {
      const documentType = button.dataset.documentOpen;

      documentPanels.forEach((panel) => {
        panel.hidden = panel.dataset.documentPanel !== documentType;
      });
      if (documentTitle) {
        documentTitle.textContent = documentType === "resume"
          ? "제출 이력서"
          : "제출 자기소개서";
      }
      documentModal?.classList.add("show");
      document.body.style.overflow = "hidden";
    });
  });

  document.querySelector("[data-document-close]")
    ?.addEventListener("click", closeDocumentModal);
  documentModal?.addEventListener("click", (event) => {
    if (event.target === documentModal) closeDocumentModal();
  });

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
        cancelButton.textContent = "지원 취소 중";
        document.querySelector("#applicationCancelForm")?.submit();
      },
    });
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeDocumentModal();
  });
});
