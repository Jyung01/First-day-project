document.addEventListener("DOMContentLoaded", () => {
  const parentSelect = document.getElementById("parentCategoryFilter");
  const childSelect = document.getElementById("childCategoryFilter");

  if (!parentSelect || !childSelect) return;

  const childOptions = [...childSelect.querySelectorAll("option[data-parent-id]")];

  const updateChildOptions = (resetSelection = false) => {
    const parentId = parentSelect.value;

    childOptions.forEach((option) => {
      const visible = !parentId || option.dataset.parentId === parentId;
      option.hidden = !visible;
      option.disabled = !visible;
    });

    if (resetSelection) {
      childSelect.value = "";
    }
  };

  parentSelect.addEventListener("change", () => {
    updateChildOptions(true);
  });

  updateChildOptions();
});
