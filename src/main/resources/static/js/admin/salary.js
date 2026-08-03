document.addEventListener("DOMContentLoaded", () => {
    salaryData.normal = salaryData.all.filter((v) => v.state === "정상");

    salaryData.review = salaryData.all.filter((v) => v.state === "검토 필요");

    salaryData.hidden = salaryData.all.filter((v) => v.state === "숨김");

    //  ==========================
    //  탭 메뉴
    //  ==========================
    const salaryBody = document.getElementById("salaryBody");
    const tabs = document.querySelectorAll(".tab-btn");

    function render(type) {
        salaryBody.innerHTML = "";

        salaryData[type].forEach((item) => {
            salaryBody.innerHTML += `

                                                    <tr>

                                                        <td>${item.no}</td>

                                                        <td>${item.company}</td>

                                                        <td>${item.job}</td>

                                                        <td>${item.career}</td>

                                                        <td>${item.salary}</td>

                                                        <td>${item.year}</td>

                                                        <td>${item.sample}</td>

                                                        <td class="${item.state === "정상" ? "normal" : item.state === "검토 필요" ? "review" : "hidden"}">

                                                            ${item.state}

                                                        </td>

                                                        <td>

                                                            <a href="#" class="open-salary-review-modal">
                                                            <!--    data-salary-id="15"-->
                                                               검토</a>

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

    //  ==========================
    //  연봉 제보 검토 모달
    //  ==========================
    const modal = document.querySelector("[data-salary-review-modal]");

    if (!modal) return;

    salaryBody.addEventListener("click", (e) => {

        const btn = e.target.closest(".open-salary-review-modal");

        if (!btn) return;

        e.preventDefault();

        // TODO
        // const salaryId = btn.dataset.salaryId;
        // fetch(`/admin/salary-report/${salaryId}`)

        modal.classList.add("is-open");

    });

    modal.querySelectorAll("[data-salary-review-close]").forEach((btn) => {

        btn.addEventListener("click", () => {
            modal.classList.remove("is-open");
        });

    });

    modal.querySelector("form").addEventListener("submit", (e) => {

        e.preventDefault();

        // TODO
        // fetch("/admin/salary-report/process")

        modal.classList.remove("is-open");

    });
});
