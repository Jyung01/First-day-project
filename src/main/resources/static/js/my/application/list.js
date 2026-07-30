document.addEventListener("DOMContentLoaded", () => {
    const filterButtons = document.querySelectorAll("[data-application-filter]");
    const applicationRows = document.querySelectorAll("[data-application-group]");
    const emptyMessage = document.querySelector("[data-application-empty]");

    filterButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const selectedGroup = button.dataset.applicationFilter;
            let visibleCount = 0;

            filterButtons.forEach((item) => item.classList.remove("is-active"));
            button.classList.add("is-active");

            applicationRows.forEach((row) => {
                const shouldShow = selectedGroup === "all"
                    || row.dataset.applicationGroup === selectedGroup;
                row.hidden = !shouldShow;
                if (shouldShow) visibleCount += 1;
            });

            if (emptyMessage) emptyMessage.hidden = visibleCount !== 0;
        });
    });
});
