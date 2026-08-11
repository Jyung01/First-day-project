document.addEventListener("DOMContentLoaded", () => {
  const initializeHierarchyFilter = (filter) => {
    const parentButtons = [
      ...filter.querySelectorAll(".category-parent-item"),
    ];
    const childGroups = [
      ...filter.querySelectorAll(".category-child-group"),
    ];

    if (parentButtons.length === 0) return;

    const showCategory = (button) => {
      const parentId = button.dataset.parentId;

      parentButtons.forEach((item) => {
        item.classList.toggle("active", item === button);
      });
      childGroups.forEach((group) => {
        group.hidden = group.dataset.categoryGroup !== parentId;
      });
    };

    const updateGroupSelectButton = (group) => {
      const button = group.querySelector("[data-group-select-all]");
      if (!button) return;

      const checkboxes = [
        ...group.querySelectorAll('input[type="checkbox"]'),
      ];
      const allChecked = checkboxes.length > 0
        && checkboxes.every((checkbox) => checkbox.checked);

      button.textContent = allChecked ? "전체 해제" : "전체 선택";
    };

    const updateAllGroupSelectButtons = () => {
      childGroups.forEach(updateGroupSelectButton);
    };

    parentButtons.forEach((button) => {
      button.addEventListener("click", () => showCategory(button));
    });

    filter.querySelectorAll("[data-group-select-all]").forEach((button) => {
      button.addEventListener("click", () => {
        const group = button.closest(".category-child-group");
        if (!group) return;

        const checkboxes = [
          ...group.querySelectorAll('input[type="checkbox"]'),
        ];
        if (checkboxes.length === 0) return;

        const allChecked = checkboxes.every(
          (checkbox) => checkbox.checked,
        );

        checkboxes.forEach((checkbox) => {
          checkbox.checked = !allChecked;
          checkbox.dispatchEvent(new Event("change", { bubbles: true }));
        });
        updateGroupSelectButton(group);
      });
    });

    childGroups.forEach((group) => {
      group.querySelectorAll('input[type="checkbox"]').forEach((checkbox) => {
        checkbox.addEventListener(
          "change",
          () => updateGroupSelectButton(group),
        );
      });
    });

    filter.querySelector("[data-filter-all]")?.addEventListener(
      "change",
      () => setTimeout(updateAllGroupSelectButtons, 0),
    );

    showCategory(parentButtons[0]);
    setTimeout(updateAllGroupSelectButtons, 0);
  };

  document.querySelectorAll(
    ".job-category-filter, .skill-category-filter",
  ).forEach(initializeHierarchyFilter);
});
