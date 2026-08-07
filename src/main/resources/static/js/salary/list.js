document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.getElementById("salaryFilterForm");
    if (!filterForm) return;

    filterForm.querySelectorAll(".salary-filter").forEach((select) => {
        select.addEventListener("change", () => filterForm.submit());
    });
});
