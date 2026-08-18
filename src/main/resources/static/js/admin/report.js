document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("[data-report-process-modal]");
    const form = document.getElementById("reportProcessForm");
    if (!modal || !form) return;
    const close = () => { modal.classList.remove("is-open"); modal.setAttribute("aria-hidden", "true"); };
    document.querySelectorAll(".open-report-process-modal").forEach((button) => button.addEventListener("click", async () => {
        try {
            const response = await fetch(`/admin/report/detail?reportId=${button.dataset.reportId}`);
            const report = await response.json();
            if (!response.ok) throw new Error(report.message || "신고 상세를 불러오지 못했습니다.");
            document.getElementById("reportId").value = report.reportId;
            document.getElementById("reportCode").textContent = `신고 RP-${report.reportId} · ${report.targetType}`;
            document.getElementById("reportTarget").textContent = report.targetName || "삭제된 대상";
            document.getElementById("reportReason").textContent = report.reasonCode;
            document.getElementById("reportDetail").textContent = report.detail || "상세 내용 없음";
            document.getElementById("reportStatus").textContent = report.status;
            document.getElementById("reportMemo").value = report.resolutionNote || "";
            form.querySelectorAll("input[name='action']").forEach((input) => input.checked = false);
            const processed = report.status !== "미처리";
            document.getElementById("reportActionField").style.display = processed ? "none" : "block";
            document.getElementById("processButton").style.display = processed ? "none" : "inline-flex";
            document.getElementById("hideActionLabel").textContent = report.targetType === "기업" ? "기업 이용정지" : "콘텐츠 숨김";
            modal.classList.add("is-open"); modal.setAttribute("aria-hidden", "false");
        } catch (error) { showReportMessage(false, error.message); }
    }));
    modal.querySelectorAll("[data-report-process-close]").forEach((button) => button.addEventListener("click", close));
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.querySelector("input[name='action']:checked")) return showReportMessage(false, "처리 방식을 선택해주세요.");
        try {
            const response = await fetch("/admin/report/process", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams(new FormData(form)) });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || "신고 처리에 실패했습니다.");
            close(); showReportMessage(true, result.message, true);
        } catch (error) { showReportMessage(false, error.message); }
    });
});
function showReportMessage(success, message, reload = false) {
    showConfirmModal({ iconClass: success ? "success" : "danger", title: success ? "처리가 완료되었습니다" : "처리할 수 없습니다", message, leftVisible: false, rightText: "확인", rightClass: "btn-primary", onRight: reload ? () => location.reload() : null });
}
