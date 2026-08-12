document.addEventListener("DOMContentLoaded", () => {
  const modal = document.getElementById("reportModal");
  if (!modal) return;

  const form = document.getElementById("reportForm");
  const targetInput = document.getElementById("reportTarget");
  const detailInput = document.getElementById("reportContent");
  const detailCount = document.getElementById("reportContentCount");
  const pendingReportKey = "pendingReportAction";

  const updateCount = () => {
    if (detailCount) detailCount.textContent = String(detailInput.value.length);
  };
  const close = () => {
    modal.classList.remove("show");
    form.reset();
    updateCount();
  };
  const openReportModal = (button) => {
    modal.dataset.reportType = button.dataset.reportType;
    modal.dataset.targetId = button.dataset.targetId;
    targetInput.value = button.dataset.targetName || "신고 대상";
    updateCount();
    modal.classList.add("show");
  };
  const moveToLogin = (reportAction) => {
    sessionStorage.setItem(
      pendingReportKey,
      JSON.stringify(reportAction),
    );
    const returnUrl = location.pathname + location.search;
    location.href = `/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`;
  };
  const showLoginModal = (button = null) => {
    const reportAction = button
      ? {
          reportType: button.dataset.reportType,
          targetId: button.dataset.targetId,
          targetName: button.dataset.targetName || "신고 대상",
        }
      : {
          reportType: modal.dataset.reportType,
          targetId: modal.dataset.targetId,
          targetName: targetInput.value || "신고 대상",
        };

    showConfirmModal({
      iconClass: "info",
      iconHtml: "?",
      title: "로그인이 필요한 기능입니다",
      message:
        "신고하려면 개인회원 로그인이 필요합니다.\n" +
        "로그인 후 다시 이용해주세요.",
      leftText: "취소",
      rightText: "로그인",
      leftClass: "btn-outline",
      rightClass: "btn-primary",
      onRight: () => moveToLogin(reportAction),
    });
  };

  document.querySelectorAll(".report-btn").forEach((button) => {
    button.addEventListener("click", () => {
      if (button.dataset.loggedIn === "false") {
        showLoginModal(button);
        return;
      }

      openReportModal(button);
    });
  });

  const resumePendingReport = () => {
    const pendingValue = sessionStorage.getItem(pendingReportKey);
    if (!pendingValue) return;

    let pendingAction;
    try {
      pendingAction = JSON.parse(pendingValue);
    } catch (error) {
      sessionStorage.removeItem(pendingReportKey);
      return;
    }

    const button = Array.from(document.querySelectorAll(".report-btn"))
      .find((item) => item.dataset.reportType === pendingAction.reportType
        && item.dataset.targetId === String(pendingAction.targetId));

    if (!button || button.dataset.loggedIn === "false") return;

    sessionStorage.removeItem(pendingReportKey);
    openReportModal(button);
  };

  resumePendingReport();

  modal.querySelector(".modal-close").addEventListener("click", close);
  modal.querySelector(".btn-cancel").addEventListener("click", close);
  modal.addEventListener("click", (event) => {
    if (event.target === modal) close();
  });
  detailInput.addEventListener("input", updateCount);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const submit = form.querySelector(".report-submit-btn");
    submit.disabled = true;

    try {
      const body = new URLSearchParams(new FormData(form));
      body.set("reportType", modal.dataset.reportType);
      body.set("targetId", modal.dataset.targetId);

      const response = await fetch("/reports", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body,
      });
      const result = await response.json();

      if (response.status === 401) {
        close();
        showLoginModal();
        return;
      }
      if (!response.ok) {
        throw new Error(result.message || "신고 접수에 실패했습니다.");
      }

      close();
      showReportResult(true, result.message || "신고가 접수되었습니다.");
    } catch (error) {
      showReportResult(
        false,
        error.message || "신고 접수에 실패했습니다.",
      );
    } finally {
      submit.disabled = false;
    }
  });
});

function showReportResult(
  success,
  message,
  onConfirm = null,
  title = null,
  confirmText = "확인",
) {
  showConfirmModal({
    iconClass: success ? "success" : "danger",
    iconHtml: success
      ? '<i class="fa-solid fa-check"></i>'
      : '<i class="fa-solid fa-exclamation"></i>',
    title: title || (success
      ? "신고가 접수되었습니다"
      : "신고를 접수할 수 없습니다"),
    message,
    leftVisible: false,
    rightText: confirmText,
    rightClass: "btn-primary",
    onRight: onConfirm,
  });
}
