document.addEventListener("DOMContentLoaded", () => {

    const reportModal = document.getElementById("reportModal");

    if (!reportModal) return;

    const reportForm = document.getElementById("reportForm");
    const closeBtn = reportModal.querySelector(".modal-close");
    const cancelBtn = reportModal.querySelector(".btn-cancel");
    const reportTarget = document.getElementById("reportTarget");
    const reportReason = document.getElementById("reportReason");
    const reportContent = document.getElementById("reportContent");
    const reportContentCount = document.getElementById("reportContentCount");
    const submitBtn = reportModal.querySelector(".report-submit-btn");

    // ==========================
    // 신고 모달 열기
    // ==========================
    document.querySelectorAll(".report-btn").forEach((btn) => {

        btn.addEventListener("click", () => {

            if (btn.dataset.loggedIn === "false") {
                showReportLoginModal();
                return;
            }

            // 신고 대상 정보 저장
            reportModal.dataset.reportType = btn.dataset.reportType;
            reportModal.dataset.targetId = btn.dataset.targetId;

            reportForm.reset();
            reportTarget.value = btn.dataset.targetName || "";
            updateContentCount();

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

    reportContent.addEventListener("input", updateContentCount);

    function updateContentCount() {
        reportContentCount.textContent = String(reportContent.value.length);
    }

    function showReportLoginModal() {
        if (typeof showConfirmModal !== "function") {
            return;
        }

        showConfirmModal({
            iconClass: "info",
            iconHtml: "?",
            title: "로그인이 필요한 기능입니다",
            message: "신고하려면 개인회원 로그인이 필요합니다.",
            leftText: "취소",
            rightText: "로그인",
            onRight: () => {
                const loginUrl = document.body.dataset.loginUrl || "/auth/login";
                const returnUrl = window.location.pathname + window.location.search;

                window.location.href =
                    `${loginUrl}?returnUrl=${encodeURIComponent(returnUrl)}`;
            }
        });
    }

    // ==========================
    // 신고 접수
    // ==========================
    reportForm.addEventListener("submit", async (e) => {

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

            case "jobPosting":
                await submitJobPostingReport(targetId);
                return;

            default:
                return;

        }

        closeReportModal();

        // TODO
        // 신고 완료 모달 호출

    });

    async function submitJobPostingReport(targetId) {
        submitBtn.disabled = true;

        try {
            const response = await fetch(
                `/api/reports/job-postings/${targetId}`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        reasonCode: reportReason.value,
                        detail: reportContent.value.trim()
                    })
                }
            );

            const result = await readResponse(response);

            if (!response.ok) {
                showReportNotice(
                    "신고를 접수할 수 없습니다",
                    result.message || "잠시 후 다시 시도해주세요."
                );
                return;
            }

            closeReportModal();
            showReportNotice(
                "신고 접수 완료",
                result.message || "신고가 접수되었습니다."
            );
        } catch (error) {
            console.error("채용공고 신고 요청 오류:", error);
            showReportNotice(
                "신고를 접수할 수 없습니다",
                "네트워크 상태를 확인한 뒤 다시 시도해주세요."
            );
        } finally {
            submitBtn.disabled = false;
        }
    }

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            return {};
        }
    }

    function showReportNotice(title, message) {
        if (typeof showConfirmModal !== "function") {
            window.alert(message);
            return;
        }

        showConfirmModal({
            iconClass: "info",
            iconHtml: '<i class="fa-solid fa-circle-info"></i>',
            title,
            message,
            leftVisible: false,
            rightText: "확인"
        });
    }

});
