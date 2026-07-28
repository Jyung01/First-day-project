document.addEventListener("DOMContentLoaded", function () {
    const tabButtons = document.querySelectorAll("[data-member-status]");
    const rows = Array.from(document.querySelectorAll("[data-member-row]"));
    const searchForm = document.querySelector("[data-member-search-form]");
    const keywordInput = document.querySelector("[data-member-keyword]");
    const empty = document.querySelector("[data-member-empty]");

    let activeStatus = "ALL";

    function filterRows() {
        const keyword = keywordInput?.value.trim().toLowerCase() ?? "";

        let visibleCount = 0;

        rows.forEach(function (row) {
            const statusMatches = activeStatus === "ALL" || row.dataset.status === activeStatus;

            const keywordMatches = !keyword || row.textContent.toLowerCase().includes(keyword);

            const visible = statusMatches && keywordMatches;

            row.hidden = !visible;

            if (visible) {
                visibleCount += 1;
            }
        });

        if (empty) {
            empty.hidden = visibleCount !== 0;
        }
    }

    tabButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            activeStatus = button.dataset.memberStatus;

            tabButtons.forEach(function (item) {
                item.classList.toggle("is-active", item === button);
            });

            filterRows();
        });
    });

    searchForm?.addEventListener("submit", function (event) {
        event.preventDefault();
        filterRows();
    });
});
