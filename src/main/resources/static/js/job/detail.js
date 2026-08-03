document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-detail-tab]").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll("[data-detail-tab]").forEach((item) => {
        item.classList.remove("active");
      });

      tab.classList.add("active");
      const isCompanyTab = tab.dataset.detailTab === "company";
      document.querySelector("[data-job-copy]")?.toggleAttribute("hidden", isCompanyTab);
      document.querySelector("[data-company-copy]")?.toggleAttribute("hidden", !isCompanyTab);
    });
  });
});
