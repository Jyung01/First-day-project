document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector(".salary-form");
    if (!form) return;

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;

        const company = form.querySelector("#companyId");
        const job = form.querySelector("#jobCategoryId");
        const career = form.querySelector("#careerYears");
        const salary = Number(form.querySelector("#baseSalary").value);
        const salaryYear = form.querySelector("#salaryYear").value;

        showConfirmModal({
            iconClass: "info",
            iconHtml: '<i class="fa-solid fa-question"></i>',
            title: "입력한 연봉정보를 등록할까요?",
            message: "입력 내용과 실제 근무한 기업인지 다시 확인해주세요.",
            extraHtml: `
                <div class="salary-summary">
                    <div class="summary-row"><span>기업</span><strong>${company.options[company.selectedIndex].text}</strong></div>
                    <div class="summary-row"><span>직무 · 경력</span><strong>${job.options[job.selectedIndex].text} · ${career.options[career.selectedIndex].text}</strong></div>
                    <div class="summary-row"><span>세전 연봉</span><strong class="point">${salary.toLocaleString()}만원</strong><span>기준연도</span><strong>${salaryYear}년</strong></div>
                </div>
                <p class="modal-desc">등록 즉시 익명 연봉 통계에 반영됩니다.</p>
            `,
            leftText: "취소",
            rightText: "등록하기",
            leftClass: "btn-outline",
            rightClass: "btn-primary",
            onRight: () => form.submit()
        });
    });
});
