document.addEventListener("DOMContentLoaded", () => {
  const modal = document.querySelector("#applicationDocumentModal");
  const title = document.querySelector("#applicationDocumentTitle");
  const panels = document.querySelectorAll("[data-document-panel]");
  const openButtons = document.querySelectorAll("[data-document-open]");
  const closeButton = document.querySelector("[data-document-close]");
  const statusButton = document.querySelector("[data-applicant-status-open]");
  const statusData = document.querySelector("[data-applicant-status-data]");
  const statusForm = document.querySelector("#applicantStatusForm");
  const nextStatusInput = document.querySelector("[data-next-status]");

  const closeModal = () => {
    modal?.classList.remove("show");
    document.body.style.overflow = "";
  };

  openButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const documentType = button.dataset.documentOpen;

      panels.forEach((panel) => {
        panel.hidden = panel.dataset.documentPanel !== documentType;
      });
      if (title) {
        title.textContent = documentType === "resume"
          ? "제출 이력서"
          : "제출 자기소개서";
      }

      modal?.classList.add("show");
      document.body.style.overflow = "hidden";
    });
  });

  closeButton?.addEventListener("click", closeModal);
  modal?.addEventListener("click", (event) => {
    if (event.target === modal) closeModal();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeModal();
  });

  const escapeHtml = (value) => String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

  statusButton?.addEventListener("click", () => {
    const currentStatus = statusData?.dataset.currentStatus || "";
    const statusOptions = Array.from(
      statusData?.querySelectorAll("[data-status-option]") || [],
    ).map((option) => option.textContent.trim());

    if (statusOptions.length === 0) return;

    const optionHtml = statusOptions
      .map((status) => `<option value="${escapeHtml(status)}">${escapeHtml(status)}</option>`)
      .join("");

    showConfirmModal({
      iconClass: "info",
      iconHtml: "✓",
      title: "지원 상태 변경",
      message:
        "현재 단계의 다음 상태 또는 불합격만 선택할 수 있습니다. 지원취소는 지원자만 가능합니다.",
      extraHtml: `
        <div class="modal-status-summary">
          <strong>현재 상태&nbsp;&nbsp;${escapeHtml(currentStatus)}</strong>
          <p>
            변경 가능&nbsp;&nbsp;현재 단계의 다음 상태 · 불합격<br>
            단계 건너뛰기 · 역방향 변경 불가
          </p>
        </div>
        <select class="modal-status-select" id="applicantStatusSelect">
          <option value="">변경할 상태를 선택해주세요</option>
          ${optionHtml}
        </select>
      `,
      leftText: "취소",
      rightText: "상태 변경",
      onRight: () => {
        const selectedStatus = document.querySelector(
          "#applicantStatusSelect",
        )?.value;

        if (!selectedStatus) return;
        showFinalStatusConfirm(selectedStatus);
      },
    });
  });

  const showFinalStatusConfirm = (selectedStatus) => {
    const applicantName = statusData?.dataset.applicantName || "지원자";
    const rejected = selectedStatus === "불합격";

    showConfirmModal({
      iconClass: rejected ? "danger" : "info",
      iconHtml: rejected ? "!" : "✓",
      title: rejected ? "불합격 처리" : "지원 상태 변경 확인",
      message: rejected
        ? `${applicantName} 지원자를 불합격 처리할까요?\n변경한 상태는 이전 단계로 되돌릴 수 없습니다.`
        : `${applicantName} 지원자의 상태를 ${selectedStatus}(으)로 변경할까요?`,
      leftText: "취소",
      rightText: rejected ? "불합격 처리" : "변경하기",
      rightClass: rejected ? "btn-danger" : "btn-primary",
      onRight: () => {
        if (!nextStatusInput || !statusForm) return;
        nextStatusInput.value = selectedStatus;
        statusForm.submit();
      },
    });
  };
});
