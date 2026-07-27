document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".bookmark").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      button.classList.toggle("active");
      button.textContent = button.classList.contains("active") ? "♥" : "♡";
      button.setAttribute("aria-pressed", String(button.classList.contains("active")));
    });
  });

  document.querySelectorAll(".filter-chip").forEach((chip) => {
    chip.addEventListener("click", () => chip.remove());
  });

  const reset = document.querySelector(".filter-reset");
  reset?.addEventListener("click", () => {
    document.querySelectorAll(".filter-chip").forEach((chip) => chip.remove());
  });

  document.querySelectorAll("[data-detail-tab]").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll("[data-detail-tab]").forEach((item) => item.classList.remove("active"));
      tab.classList.add("active");
      const company = tab.dataset.detailTab === "company";
      document.querySelector("[data-job-copy]")?.toggleAttribute("hidden", company);
      document.querySelector("[data-company-copy]")?.toggleAttribute("hidden", !company);
    });
  });

  document.querySelectorAll('input[name="resumeId"]').forEach((radio) => {
    radio.addEventListener("change", () => {
      document.querySelectorAll(".selected-label").forEach((label) => label.textContent = "선택");
      radio.closest(".resume-option")?.querySelector(".selected-label").replaceChildren("선택됨");
    });
  });
});
