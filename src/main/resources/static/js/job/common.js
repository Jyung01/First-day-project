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

  const filterParameterNames = [
    "categoryId",
    "region",
    "career",
    "education",
    "skillId",
  ];
  const currentParameters = new URLSearchParams(window.location.search);
  const selectedParentCategoryId = currentParameters.get("parentCategoryId");
  const categoryFilter = filterRoot?.querySelector(".job-category-filter");

  if (selectedParentCategoryId && categoryFilter) {
    const selectedParent = [...categoryFilter.querySelectorAll(".category-parent-item")]
      .find((button) => button.dataset.parentId === selectedParentCategoryId);
    if (selectedParent) {
      categoryFilter.dataset.selectedParentLabel = selectedParent.textContent.trim();
    }
  }

  filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
    const choices = [
      ...group.querySelectorAll(
        'input[type="checkbox"]:not([data-filter-all])',
      ),
    ];
    const parameterName = choices[0]?.name;
    if (!filterParameterNames.includes(parameterName)) return;

    const selectedValues = currentParameters.getAll(parameterName);
    if (selectedValues.length === 0) return;

    const all = group.querySelector("[data-filter-all]");
    all.checked = false;
    choices.forEach((choice) => {
      choice.checked = selectedValues.includes(choice.value);
    });
    all.indeterminate = false;
  });

  const buildFilterParameters = () => {
    const parameters = new URLSearchParams(window.location.search);
    filterParameterNames.forEach((name) => parameters.delete(name));
    parameters.delete("parentCategoryId");
    parameters.delete("page");

    filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
      const all = group.querySelector("[data-filter-all]");
      const choices = [
        ...group.querySelectorAll(
          'input[type="checkbox"]:not([data-filter-all])',
        ),
      ];

      if (!choices.some((choice) => choice.checked)) return;

      choices
        .filter((choice) => choice.checked)
        .forEach((choice) => parameters.append(choice.name, choice.value));
    });

    return parameters;
  };

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
    const selected = [
      ...filterRoot.querySelectorAll(
        'input[type="checkbox"]:checked:not([data-filter-all])',
      ),
    ];

    selected.forEach((input) => {
      const chip = document.createElement("button");
      chip.type = "button";
      chip.className = "filter-chip";
      chip.textContent = `${input.dataset.label} ×`;
      chip.addEventListener("click", () => {
        input.checked = false;
        input.dispatchEvent(new Event("change", { bubbles: true }));
      });
      filterChips.append(chip);
    });

    const selectedParentLabel = categoryFilter?.dataset.selectedParentLabel;
    if (selectedParentLabel) {
      const chip = document.createElement("button");
      chip.type = "button";
      chip.className = "filter-chip";
      chip.textContent = `${selectedParentLabel} ×`;
      chip.addEventListener("click", () => {
        categoryFilter.dataset.selectedParentLabel = "";
        currentParameters.delete("parentCategoryId");
        window.location.href = `/job/search?${currentParameters.toString()}`;
      });
      filterChips.prepend(chip);
    }

    activeFilters.hidden = selected.length === 0 && !selectedParentLabel;
  };

  const updateFilterToggle = (group) => {
    const label = group.dataset.filterLabel;
    const count = group.querySelectorAll('input:checked:not([data-filter-all])').length;
    const selectedParentLabel = group.dataset.selectedParentLabel;
    group.querySelector(".multi-filter-toggle").firstChild.textContent =
      count === 0 && selectedParentLabel
        ? `${selectedParentLabel} `
        : count === 0 ? `${label} 전체 ` : `${label} ${count}개 `;
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
      if (all.checked) {
        choices.forEach((choice) => choice.checked = false);
      } else if (!choices.some((choice) => choice.checked)) {
        all.checked = true;
      }
      all.indeterminate = false;
      updateFilterToggle(group);
      renderActiveFilters();
    });

    choices.forEach((choice) => {
      choice.addEventListener("change", () => {
        if (group.classList.contains("job-category-filter")) {
          group.dataset.selectedParentLabel = "";
        }
        const selectedCount = choices.filter((item) => item.checked).length;
        all.checked = selectedCount === 0;
        all.indeterminate = false;
        updateFilterToggle(group);
        renderActiveFilters();
      });
    });

    group.querySelector(".multi-filter-apply").addEventListener("click", () => {
      updateFilterToggle(group);
      renderActiveFilters();
      closeFilterPanels();
    });
  });

  filterRoot?.querySelectorAll("[data-filter-group]").forEach(
    updateFilterToggle,
  );
  renderActiveFilters();

  document.querySelector(".job-search")?.addEventListener("submit", (event) => {
    event.preventDefault();

    const parameters = buildFilterParameters();
    const keyword = event.currentTarget.elements.keyword.value.trim();

    if (keyword) {
      parameters.set("keyword", keyword);
    } else {
      parameters.delete("keyword");
    }

    window.location.href = `/job/search?${parameters.toString()}`;
  });

  document.querySelectorAll("[data-job-sort]").forEach((button) => {
    button.addEventListener("click", () => {
      const parameters = new URLSearchParams(window.location.search);
      parameters.set("sort", button.dataset.jobSort);
      parameters.delete("page");
      window.location.href = `/job/search?${parameters.toString()}`;
    });
  });

  document.addEventListener("click", async (event) => {
    const pageLink = event.target.closest(".pagination a");
    if (
      !pageLink
      || pageLink.classList.contains("is-disabled")
      || pageLink.getAttribute("href") === "#"
    ) {
      return;
    }

    event.preventDefault();

    try {
      const response = await fetch(pageLink.href, {
        headers: { "X-Requested-With": "XMLHttpRequest" },
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const documentHtml = new DOMParser().parseFromString(
        await response.text(),
        "text/html",
      );
      const nextGrid = documentHtml.querySelector(".job-card-grid");
      const nextPagination = documentHtml.querySelector(".pagination");
      const currentGrid = document.querySelector(".job-card-grid");
      const currentPagination = document.querySelector(".pagination");
      const nextListPane = documentHtml.querySelector(".job-list-pane");
      const currentListPane = document.querySelector(".job-list-pane");

      if (nextListPane && currentListPane && nextPagination && currentPagination) {
        currentListPane.replaceWith(nextListPane);
        currentPagination.replaceWith(nextPagination);
        window.history.pushState({}, "", pageLink.href);
        document.dispatchEvent(new CustomEvent("job:list-updated"));
        return;
      }

      if (!nextGrid || !nextPagination || !currentGrid || !currentPagination) {
        window.location.href = pageLink.href;
        return;
      }

      currentGrid.replaceWith(nextGrid);
      currentPagination.replaceWith(nextPagination);
      window.history.pushState({}, "", pageLink.href);
      document.dispatchEvent(new CustomEvent("job:list-updated"));
      document.querySelector(".job-content")?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    } catch (error) {
      window.location.href = pageLink.href;
    }
  });

  document.querySelector("[data-filter-reset]")?.addEventListener("click", () => {
    filterRoot?.querySelectorAll("[data-filter-group]").forEach((group) => {
      const all = group.querySelector("[data-filter-all]");
      group.querySelectorAll(
        'input[type="checkbox"]:not([data-filter-all])',
      ).forEach((input) => input.checked = false);
      all.checked = true;
      all.indeterminate = false;
      updateFilterToggle(group);
    });
    renderActiveFilters();
  });

  document.addEventListener("click", (event) => {
    if (!event.target.closest("[data-filter-group]")) closeFilterPanels();
  });

});
