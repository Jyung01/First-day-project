document.addEventListener("DOMContentLoaded", () => {
  const filterButtons = document.querySelectorAll("[data-job-filter]");
  const jobRows = document.querySelectorAll("[data-job-status]");

  filterButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const selectedStatus = button.dataset.jobFilter;

      filterButtons.forEach((item) => item.classList.remove("is-active"));
      button.classList.add("is-active");

      jobRows.forEach((row) => {
        row.hidden = selectedStatus !== "ALL"
          && row.dataset.jobStatus !== selectedStatus;
      });
    });
  });
});
