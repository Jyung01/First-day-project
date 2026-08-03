document.addEventListener("DOMContentLoaded", () => {
  const jobTable = document.querySelector(".job-table");
  const filterButtons = document.querySelectorAll("[data-job-filter]");
  const jobRows = document.querySelectorAll("[data-job-status]");

  if (!jobTable) return;

  const filterJobs = (status) => {
    jobRows.forEach((row) => {
      row.hidden = status !== "ALL" && row.dataset.jobStatus !== status;
    });
  };

  filterButtons.forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      filterButtons.forEach((item) => item.classList.remove("is-active"));
      button.classList.add("is-active");
      filterJobs(button.dataset.jobFilter);
    });
  });

  const getJobRow = (button) => button.closest(".job-row");

  const getJobTitle = (row) =>
    row?.querySelector("strong")?.textContent.trim() || "선택한 채용공고";

  const closeJob = (row, button) => {
    const badge = row?.querySelector(".corp-badge");

    if (badge) {
      badge.textContent = "마감";
      badge.className = "corp-badge";
    }

    if (row) {
      row.dataset.jobStatus = "CLOSED";
    }

    button.remove();
  };

  const openCloseModal = (button) => {
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
      onRight: () => closeJob(row, button),
    });
  };

  const openDeleteModal = (button) => {
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
      onRight: () => row?.remove(),
    });
  };

  jobTable.addEventListener("click", (event) => {
    const closeButton = event.target.closest(".job-action--close");
    const deleteButton = event.target.closest(".job-action--delete");

    if (closeButton) {
      openCloseModal(closeButton);
      return;
    }

    if (deleteButton) {
      openDeleteModal(deleteButton);
    }
  });
});
