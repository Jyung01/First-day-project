document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-detail-tab]").forEach((tab) => {
    tab.addEventListener("click", () => {
      const tabs = tab.closest(".detail-tabs");
      const previousTop = tabs?.getBoundingClientRect().top;

      document.querySelectorAll("[data-detail-tab]").forEach((item) => {
        item.classList.remove("active");
      });

      tab.classList.add("active");
      const isCompanyTab = tab.dataset.detailTab === "company";
      document.querySelector("[data-job-copy]")?.toggleAttribute("hidden", isCompanyTab);
      document.querySelector("[data-company-copy]")?.toggleAttribute("hidden", !isCompanyTab);

      if (tabs && previousTop !== undefined) {
        requestAnimationFrame(() => {
          const currentTop = tabs.getBoundingClientRect().top;
          window.scrollBy(0, currentTop - previousTop);
        });
      }
    });
  });
});
