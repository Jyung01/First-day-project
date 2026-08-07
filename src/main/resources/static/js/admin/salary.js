document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("[data-salary-review-modal]");
    const form = document.getElementById("salaryReviewForm");
    if (!modal || !form) return;
    const status = document.getElementById("reviewStatus");
    const reasonField = document.getElementById("hiddenReasonField");
    const reason = document.getElementById("reviewHiddenReason");
    const toggleReason = () => reasonField.style.display = status.value === "숨김" ? "block" : "none";
    status.addEventListener("change", toggleReason);

    document.querySelectorAll(".open-salary-review-modal").forEach(button => {
        button.addEventListener("click", async () => {
            try {
                const response = await fetch(`/admin/salary/detail?salaryRecordId=${encodeURIComponent(button.dataset.salaryId)}`);
                const data = await response.json();
                if (!response.ok) throw new Error(data.message || "연봉정보를 불러오지 못했습니다.");
                document.getElementById("reviewSalaryRecordId").value = data.salaryRecordId;
                document.getElementById("reviewCompany").value = data.companyName || "-";
                document.getElementById("reviewJob").value = data.categoryName || "-";
                document.getElementById("reviewCareer").value = data.careerYears === 0 ? "신입" : `${data.careerYears}년`;
                document.getElementById("reviewSalary").value = `${Number(data.baseSalary).toLocaleString()}만원`;
                document.getElementById("reviewYear").value = data.salaryYear;
                document.getElementById("reviewEmployment").value = `${data.employmentStatus} · ${data.employmentType}`;
                status.value = data.status === "숨김" ? "숨김" : "정상";
                reason.value = data.hiddenReason || "";
                toggleReason();
                modal.classList.add("is-open");
                modal.setAttribute("aria-hidden", "false");
            } catch (error) { showSalaryMessage(false, error.message); }
        });
    });

    modal.querySelectorAll("[data-salary-review-close]").forEach(button =>
        button.addEventListener("click", () => closeSalaryReviewModal(modal)));

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (status.value === "숨김" && !reason.value.trim()) {
            showSalaryMessage(false, "숨김 사유를 입력해주세요.");
            return;
        }
        try {
            const response = await fetch("/admin/salary/review", {
                method: "POST", headers: {"Content-Type": "application/x-www-form-urlencoded"},
                body: new URLSearchParams(new FormData(form))
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || "검토 결과를 저장하지 못했습니다.");
            closeSalaryReviewModal(modal);
            showSalaryMessage(true, result.message, true);
        } catch (error) { showSalaryMessage(false, error.message); }
    });
});

function closeSalaryReviewModal(modal) {
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
}

function showSalaryMessage(success, message, reload = false) {
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        iconHtml: success ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-exclamation"></i>',
        title: success ? "처리가 완료되었습니다" : "처리할 수 없습니다", message,
        leftVisible: false, rightText: "확인", rightClass: "btn-primary",
        onRight: reload ? () => location.reload() : null
    });
}
