document.addEventListener("DOMContentLoaded", () => {
    // 상태별 데이터 생성
    reportData.waiting = reportData.all.filter((v) => v.state === "미처리");

    reportData.complete = reportData.all.filter((v) => v.state === "처리완료");

    reportData.reject = reportData.all.filter((v) => v.state === "기각");

    // =============================
    // 테이블 바디 채우기
    // =============================
    const reportBody = document.getElementById("reportBody");
    const tabs = document.querySelectorAll(".tab-btn");

    function render(type) {
        reportBody.innerHTML = "";

        reportData[type].forEach((item) => {
            reportBody.innerHTML += `
          <tr>
            <td>${item.no}</td>
            <td>${item.type}</td>
            <td>${item.target}</td>
            <td>${item.reason}</td>
            <td>${item.writer}</td>
            <td>${item.date}</td>
            <td class="${item.state === "미처리" ? "waiting" : item.state === "처리완료" ? "complete" : "reject"}">
              ${item.state}
            </td>
            <td>
              <a href="#" class="open-report-process-modal">처리</a>
<!--              data-report-id="91"-->
            </td>
          </tr>
        `;
        });
    }

    // 최초 출력
    render("all");

    // 탭 클릭 이벤트
    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((btn) => btn.classList.remove("active"));
            tab.classList.add("active");

            render(tab.dataset.tab);
        });
    });

    // =============================
    // 신고 처리 모달
    // =============================
    const reportProcessModal = document.querySelector("[data-report-process-modal]");

    if (!reportProcessModal) return;

    // 신고 처리 버튼 클릭 (이벤트 위임)
    reportBody.addEventListener("click", (e) => {

        const btn = e.target.closest(".open-report-process-modal");

        if (!btn) return;

        e.preventDefault();

        // TODO
        // const reportId = btn.dataset.reportId;
        // fetch(`/admin/report/${reportId}`)

        reportProcessModal.classList.add("is-open");

    });

    // 닫기 버튼 / 배경 클릭
    reportProcessModal
        .querySelectorAll("[data-report-process-close]")
        .forEach((btn) => {
            btn.addEventListener("click", () => {
                reportProcessModal.classList.remove("is-open");
            });
        });

    // 처리 완료
    reportProcessModal.querySelector("form").addEventListener("submit", (e) => {
        e.preventDefault();

        // TODO
        // fetch("/admin/report/process", {...})

        reportProcessModal.classList.remove("is-open");
    });
});
