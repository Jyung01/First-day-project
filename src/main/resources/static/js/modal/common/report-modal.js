document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("reportModal");
    if (!modal) return;
    const form = document.getElementById("reportForm");
    const targetInput = document.getElementById("reportTarget");
    const close = () => { modal.classList.remove("show"); form.reset(); };

    document.querySelectorAll(".report-btn").forEach((button) => button.addEventListener("click", () => {
        modal.dataset.reportType = button.dataset.reportType;
        modal.dataset.targetId = button.dataset.targetId;
        targetInput.value = button.dataset.targetName || "신고 대상";
        modal.classList.add("show");
    }));
    modal.querySelector(".modal-close").addEventListener("click", close);
    modal.querySelector(".btn-cancel").addEventListener("click", close);
    modal.addEventListener("click", (event) => { if (event.target === modal) close(); });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const submit = form.querySelector(".report-submit-btn");
        submit.disabled = true;
        try {
            const body = new URLSearchParams(new FormData(form));
            body.set("reportType", modal.dataset.reportType);
            body.set("targetId", modal.dataset.targetId);
            const response = await fetch("/reports", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body });
            const result = await response.json();
            if (response.status === 401) {
                close();
                showReportResult(false, result.message || "로그인이 필요합니다.", () => {
                    location.href = `/auth/login?redirect=${encodeURIComponent(location.pathname + location.search)}`;
                }, "로그인이 필요합니다", "로그인");
                return;
            }
            if (!response.ok) throw new Error(result.message || "신고 접수에 실패했습니다.");
            close();
            showReportResult(true, result.message || "신고가 접수되었습니다.");
        } catch (error) {
            showReportResult(false, error.message || "신고 접수에 실패했습니다.");
        }
        finally { submit.disabled = false; }
    });
});

function showReportResult(success, message, onConfirm = null, title = null, confirmText = "확인") {
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        iconHtml: success
            ? '<i class="fa-solid fa-check"></i>'
            : '<i class="fa-solid fa-exclamation"></i>',
        title: title || (success ? "신고가 접수되었습니다" : "신고를 접수할 수 없습니다"),
        message,
        leftVisible: false,
        rightText: confirmText,
        rightClass: "btn-primary",
        onRight: onConfirm
    });
}
