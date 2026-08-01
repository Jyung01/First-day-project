document.addEventListener("DOMContentLoaded", () => {
  const statusButton = document.querySelector("[data-applicant-status-open]");
  const statusBadges = document.querySelectorAll(".corp-badge");

  statusButton?.addEventListener("click", () => {
    const currentStatus = statusBadges[0]?.textContent.trim() || "서류검토중";

    showConfirmModal({
      iconClass: "info",
      iconHtml: "✓",
      title: "지원 상태 변경",
      message:
        "현재 단계의 다음 상태 또는 불합격만 선택할 수 있습니다.\n지원취소는 지원자만 가능합니다.",
      extraHtml: `
        <div class="modal-status-summary">
          <strong>
            현재 상태&nbsp;&nbsp;<span data-current-applicant-status></span>
          </strong>
          <p>
            변경 가능&nbsp;&nbsp;현재 단계의 다음 상태 · 불합격<br />
            단계 건너뛰기 · 역방향 변경 불가 · 지원취소는 지원자만 가능
          </p>
        </div>
        <select
          class="modal-status-select"
          id="applicantStatusSelect"
          aria-label="변경할 지원 상태"
        >
          <option value="서류합격">서류합격</option>
          <option value="불합격">불합격</option>
        </select>
      `,
      leftText: "취소",
      rightText: "상태 변경",
      onRight: () => {
        const selectedStatus = document.querySelector(
          "#applicantStatusSelect",
        )?.value;

        if (!selectedStatus) return;
        statusBadges.forEach((badge) => {
          badge.textContent = selectedStatus;
        });
      },
    });

    const currentStatusLabel = document.querySelector(
      "[data-current-applicant-status]",
    );
    if (currentStatusLabel) {
      currentStatusLabel.textContent = currentStatus;
    }
  });
});
