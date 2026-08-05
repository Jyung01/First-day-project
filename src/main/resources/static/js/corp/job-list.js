document.addEventListener("DOMContentLoaded", () => {
  const jobTable = document.querySelector(".job-table");

  if (!jobTable) return;

  const getJobRow = (button) => button.closest(".job-row");

  const getJobTitle = (row) =>
    row?.querySelector("strong")?.textContent.trim() || "선택한 채용공고";

  const setHiddenReasonModalStyle = (enabled) => {
    document
      .querySelector("#confirmModal")
      ?.classList.toggle("corp-hidden-reason-modal", enabled);
  };

  const openCloseModal = (button) => {
    setHiddenReasonModalStyle(false);
    const row = getJobRow(button);
    const jobTitle = getJobTitle(row);

    showConfirmModal({
      iconClass: "info",
      iconHtml: "✓",
      title: "채용공고 마감",
      message: `“${jobTitle}” 공고를 마감할까요?`,
      extraHtml: `
        <div class="corp-job-modal-notice">
          <strong>마감 후 신규 지원은 받을 수 없습니다.</strong>
          <span>기존 지원자와 지원 내역은 유지됩니다.</span>
        </div>
      `,
      leftText: "취소",
      rightText: "공고 마감",
      rightClass: "btn-primary",
      onRight: () => {
        document
          .getElementById(button.dataset.closeFormId)
          ?.requestSubmit();
      },
    });
  };

  const openDeleteModal = (button) => {
    setHiddenReasonModalStyle(false);
    const row = getJobRow(button);
    const jobTitle = getJobTitle(row);

    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "공고 삭제 확인",
      message: `“${jobTitle}” 채용공고를 삭제할까요?`,
      extraHtml: `
        <div class="corp-job-modal-notice corp-job-modal-notice--danger">
          <strong>임시저장·마감 공고만 삭제할 수 있습니다.</strong>
          <span>마감 공고의 기존 지원 내역은 유지됩니다.</span>
        </div>
      `,
      leftText: "취소",
      rightText: "공고 삭제",
      rightClass: "btn-danger",
      onRight: () => {
        document
          .getElementById(button.dataset.deleteFormId)
          ?.requestSubmit();
      },
    });
  };

  const openHiddenReasonModal = (button) => {
    const reason = button.dataset.hiddenReason?.trim()
      || "등록된 숨김 사유가 없습니다.";

    showConfirmModal({
      iconClass: "info",
      iconHtml: "!",
      title: "숨김 사유 확인",
      message: reason,
      extraHtml: `
        <div class="corp-job-modal-notice">
          <strong>숨김 사유를 확인한 후 내용을 수정해 주세요.</strong>
          <span>수정이 끝나면 관리자에게 재검토를 요청할 수 있습니다.</span>
        </div>
      `,
      leftText: "취소",
      rightText: "공고 수정",
      rightClass: "btn-primary",
      onRight: () => window.location.assign(button.dataset.editUrl),
    });

    setHiddenReasonModalStyle(true);
  };

  jobTable.addEventListener("click", (event) => {
    const closeButton = event.target.closest(".job-action--close");
    const deleteButton = event.target.closest(".job-action--delete");
    const hiddenEditButton = event.target.closest(
      ".job-action--hidden-edit",
    );

    if (hiddenEditButton) {
      openHiddenReasonModal(hiddenEditButton);
      return;
    }

    if (closeButton) {
      openCloseModal(closeButton);
      return;
    }

    if (deleteButton) {
      openDeleteModal(deleteButton);
    }
  });
});
