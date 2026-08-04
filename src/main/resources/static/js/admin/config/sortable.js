document.addEventListener("DOMContentLoaded", () => {
  let draggedItem = null;
  let draggedList = null;

  const clearDragOver = (list) => {
    list
      .querySelectorAll("[data-sort-item].is-drag-over")
      .forEach((item) => item.classList.remove("is-drag-over"));
  };

  const getItemAfterPointer = (list, pointerY) => {
    const items = Array.from(
      list.querySelectorAll("[data-sort-item]:not(.is-dragging)"),
    );

    return items.reduce(
      (closest, item) => {
        const box = item.getBoundingClientRect();
        const offset = pointerY - box.top - box.height / 2;

        if (offset < 0 && offset > closest.offset) {
          return { offset, item };
        }

        return closest;
      },
      { offset: Number.NEGATIVE_INFINITY, item: null },
    ).item;
  };

  const submitOrder = (list, orderedIds) => {
    const form = document.getElementById(list.dataset.sortFormId);

    if (!form || orderedIds.length === 0) return;

    form.querySelectorAll("[data-sort-generated]").forEach((input) => {
      input.remove();
    });

    const parentInput = form.querySelector("[data-sort-form-parent]");

    if (parentInput) {
      parentInput.value = list.dataset.sortParentId || "";
    }

    orderedIds.forEach((id) => {
      const input = document.createElement("input");

      input.type = "hidden";
      input.name = "orderedIds";
      input.value = id;
      input.dataset.sortGenerated = "true";
      form.appendChild(input);
    });

    form.submit();
  };

  document.querySelectorAll("[data-sort-list]").forEach((list) => {
    list.addEventListener("pointerdown", (event) => {
      const handle = event.target.closest(".config-drag-handle");
      const item = handle?.closest("[data-sort-item]");

      if (!item || item.parentElement !== list) return;

      item.draggable = true;
    });

    list.addEventListener("dragstart", (event) => {
      const item = event.target.closest("[data-sort-item]");

      if (!item?.draggable || item.parentElement !== list) {
        event.preventDefault();
        return;
      }

      draggedItem = item;
      draggedList = list;
      item.classList.add("is-dragging");
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", item.dataset.sortId || "");
    });

    list.addEventListener("dragover", (event) => {
      if (!draggedItem || draggedList !== list) return;

      event.preventDefault();
      event.dataTransfer.dropEffect = "move";

      const nextItem = getItemAfterPointer(list, event.clientY);
      const emptyMessage = list.querySelector(":scope > .config-empty");

      clearDragOver(list);

      if (nextItem) {
        nextItem.classList.add("is-drag-over");
        list.insertBefore(draggedItem, nextItem);
        return;
      }

      list.insertBefore(draggedItem, emptyMessage);
    });

    list.addEventListener("drop", (event) => {
      if (draggedList === list) {
        event.preventDefault();
      }
    });

    list.addEventListener("dragend", () => {
      if (!draggedItem || draggedList !== list) return;

      const orderedIds = Array.from(
        list.querySelectorAll(":scope > [data-sort-item]"),
      ).map((item) => Number(item.dataset.sortId));

      draggedItem.classList.remove("is-dragging");
      draggedItem.draggable = false;
      clearDragOver(list);

      list.dispatchEvent(new CustomEvent("config:sort-changed", {
        bubbles: true,
        detail: { orderedIds },
      }));

      submitOrder(list, orderedIds);

      draggedItem = null;
      draggedList = null;
    });
  });
});
