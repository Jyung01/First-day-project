document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".filter-chip").forEach((chip) => {
    chip.addEventListener("click", () => chip.remove());
  });

  const reset = document.querySelector(".filter-reset");
  reset?.addEventListener("click", () => {
    document.querySelectorAll(".filter-chip").forEach((chip) => chip.remove());
  });

  const filterRoot = document.querySelector("[data-multi-filters]");
  const activeFilters = document.querySelector("[data-active-filters]");
  const filterChips = document.querySelector("[data-filter-chips]");

  const closeFilterPanels = (except = null) => {
    filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
      if (group === except) return;
      group.querySelector(".multi-filter-panel").hidden = true;
      group.querySelector(".multi-filter-toggle").setAttribute("aria-expanded", "false");
    });
  };

  const renderActiveFilters = () => {
    if (!filterRoot || !activeFilters || !filterChips) return;
    filterChips.replaceChildren();
    const selected = [...filterRoot.querySelectorAll('input[type="checkbox"]:checked:not([data-filter-all])')]
      .filter((input) => !input.closest("[data-filter-group]").querySelector("[data-filter-all]").checked);

    selected.forEach((input) => {
      const chip = document.createElement("button");
      chip.type = "button";
      chip.className = "filter-chip";
      chip.textContent = `${input.dataset.label} ×`;
      chip.addEventListener("click", () => {
        input.checked = false;
        const group = input.closest("[data-filter-group]");
        const all = group.querySelector("[data-filter-all]");
        const choices = [...group.querySelectorAll('input[type="checkbox"]:not([data-filter-all])')];
        all.checked = choices.every((choice) => choice.checked);
        all.indeterminate = choices.some((choice) => choice.checked) && !all.checked;
        updateFilterToggle(group);
        renderActiveFilters();
      });
      filterChips.append(chip);
    });

    activeFilters.hidden = selected.length === 0;
  };

  const updateFilterToggle = (group) => {
    const label = group.dataset.filterLabel;
    const allSelected = group.querySelector("[data-filter-all]").checked;
    const count = group.querySelectorAll('input:checked:not([data-filter-all])').length;
    group.querySelector(".multi-filter-toggle").firstChild.textContent =
      allSelected ? `${label} 전체 ` : `${label} ${count}개 `;
  };

  filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
    const toggle = group.querySelector(".multi-filter-toggle");
    const panel = group.querySelector(".multi-filter-panel");
    const all = group.querySelector("[data-filter-all]");
    const choices = [...group.querySelectorAll('input[type="checkbox"]:not([data-filter-all])')];

    toggle.addEventListener("click", () => {
      const willOpen = panel.hidden;
      closeFilterPanels(group);
      panel.hidden = !willOpen;
      toggle.setAttribute("aria-expanded", String(willOpen));
    });

    all.addEventListener("change", () => {
      choices.forEach((choice) => choice.checked = all.checked);
      all.indeterminate = false;
    });

    choices.forEach((choice) => {
      choice.addEventListener("change", () => {
        const selectedCount = choices.filter((item) => item.checked).length;
        all.checked = selectedCount === choices.length;
        all.indeterminate = selectedCount > 0 && selectedCount < choices.length;
      });
    });

    if (all.checked) choices.forEach((choice) => choice.checked = true);

    group.querySelector(".multi-filter-apply").addEventListener("click", () => {
      updateFilterToggle(group);
      renderActiveFilters();
      closeFilterPanels();
    });
  });

  document.querySelector("[data-filter-reset]")?.addEventListener("click", () => {
    filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
      group.querySelectorAll('input[type="checkbox"]').forEach((input) => input.checked = true);
      group.querySelector("[data-filter-all]").indeterminate = false;
      updateFilterToggle(group);
    });
    renderActiveFilters();
  });

  document.addEventListener("click", (event) => {
    if (!event.target.closest("[data-filter-group]")) closeFilterPanels();
  });

});
