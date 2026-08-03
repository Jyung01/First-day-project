document.addEventListener("DOMContentLoaded", () => {

    const reportModal = document.getElementById("reportModal");

    if (!reportModal) return;

    const reportForm = document.getElementById("reportForm");
    const closeBtn = reportModal.querySelector(".modal-close");
    const cancelBtn = reportModal.querySelector(".btn-cancel");

    // ==========================
    // 신고 모달 열기
    // ==========================
    document.querySelectorAll(".report-btn").forEach((btn) => {

        btn.addEventListener("click", () => {

            // 신고 대상 정보 저장
            reportModal.dataset.reportType = btn.dataset.reportType;
            reportModal.dataset.targetId = btn.dataset.targetId;

            // TODO
            // 신고 대상명 표시
            // document.getElementById("reportTarget").value = btn.dataset.targetName;

            reportModal.classList.add("show");

        });

    });

    // ==========================
    // 모달 닫기
    // ==========================
    function closeReportModal() {
        reportModal.classList.remove("show");
    }

    closeBtn.addEventListener("click", closeReportModal);

    cancelBtn.addEventListener("click", closeReportModal);

    reportModal.addEventListener("click", (e) => {

        if (e.target === reportModal) {
            closeReportModal();
        }

    });

    // ==========================
    // 신고 접수
    // ==========================
    reportForm.addEventListener("submit", (e) => {

        e.preventDefault();

        const reportType = reportModal.dataset.reportType;
        const targetId = reportModal.dataset.targetId;

        switch (reportType) {

            case "company":

                // TODO
                // 기업 신고 fetch
                // POST /reports/company

                break;

            case "companyReview":

                // TODO
                // 기업리뷰 신고 fetch
                // POST /reports/company-review

                break;

            case "interviewReview":

                // TODO
                // 면접후기 신고 fetch
                // POST /reports/interview-review

                break;

            default:
                return;

        }

        closeReportModal();

        // TODO
        // 신고 완료 모달 호출

    });

});