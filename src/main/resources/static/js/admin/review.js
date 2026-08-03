document.addEventListener("DOMContentLoaded", function () {
    const tbody = document.getElementById("reviewBody");
    const tabs = document.querySelectorAll(".tab-btn");

    function render(type) {
        tbody.innerHTML = "";

        reviewData[type].forEach((item) => {
            tbody.innerHTML += `
            <tr>

                <td>${item.no}</td>

                <td>${item.company}</td>

                <td>${item.title}</td>

                <td>${item.writer}</td>

                <td>${item.score}</td>

                <td>${item.report}</td>

                <td>${item.date}</td>

                <td>
                    <a href="#"  class="open-review-detail-modal">상세</a>
<!--                     data-review-id="3238"-->
                </td>

            </tr>
        `;
        });
    }

    render("company");

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((btn) => btn.classList.remove("active"));

            tab.classList.add("active");

            render(tab.dataset.tab);
        });
    });

    // ===============================
    //  후기 관리 모달
    // ===============================
    const modal = document.querySelector("[data-review-detail-modal]");

    if (!modal) return;

    tbody.addEventListener("click", (e) => {

        const btn = e.target.closest(".open-review-detail-modal");

        if (!btn) return;

        e.preventDefault();

        // TODO
        // const reviewId = btn.dataset.reviewId;
        // fetch(`/admin/reviews/${reviewId}`)

        modal.classList.add("is-open");

    });

    modal.querySelectorAll("[data-review-detail-close]").forEach((btn) => {

        btn.addEventListener("click", () => {
            modal.classList.remove("is-open");
        });

    });

    modal.querySelector("form").addEventListener("submit", (e) => {

        e.preventDefault();

        // TODO
        // fetch("/admin/reviews/hide")

        modal.classList.remove("is-open");

    });

});
