document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-benefit-modal]");
    const openButtons = document.querySelectorAll("[data-benefit-modal-open]");

    if (!modal || openButtons.length === 0) {
        return;
    }

    const MAX_BENEFITS = 10;

    const closeButtons = modal.querySelectorAll("[data-benefit-modal-close]");
    const cancelButton = modal.querySelector("[data-benefit-modal-cancel]");
    const confirmButton = modal.querySelector("[data-benefit-modal-confirm]");

    const optionCheckboxes = Array.from(modal.querySelectorAll("[data-benefit-option]"));
    const customInput = modal.querySelector("[data-benefit-custom-input]");
    const customAddButton = modal.querySelector("[data-benefit-custom-add]");

    const modalSelectedList = modal.querySelector("[data-benefit-modal-selected-list]");
    const modalCount = modal.querySelector("[data-benefit-modal-count]");
    const confirmCount = modal.querySelector("[data-benefit-confirm-count]");
    const modalMessage = modal.querySelector("[data-benefit-modal-message]");

    const pageChipList = document.querySelector("[data-benefit-chip-list]");
    const pageEmpty = document.querySelector("[data-benefit-empty]");
    const hiddenInputs = document.querySelector("[data-benefit-hidden-inputs]");
    const pageCount = document.querySelector("[data-benefit-selection-count]");

    let confirmedBenefits = readPageBenefits();
    let temporaryBenefits = [];
    let previouslyFocusedElement = null;

    function readPageBenefits() {
        return Array.from(pageChipList?.querySelectorAll("[data-benefit-value]") ?? []).map(function (chip) {
            return chip.dataset.benefitValue;
        });
    }

    function showMessage(message) {
        if (!modalMessage) {
            return;
        }

        modalMessage.textContent = message;
        modalMessage.classList.add("is-visible");
    }

    function clearMessage() {
        if (!modalMessage) {
            return;
        }

        modalMessage.textContent = "";
        modalMessage.classList.remove("is-visible");
    }

    function updateCounts() {
        const count = temporaryBenefits.length;

        if (modalCount) {
            modalCount.textContent = String(count);
        }

        if (confirmCount) {
            confirmCount.textContent = String(count);
        }
    }

    function synchronizeCheckboxes() {
        optionCheckboxes.forEach(function (checkbox) {
            checkbox.checked = temporaryBenefits.includes(checkbox.value);
        });
    }

    function renderModalSelectedBenefits() {
        if (!modalSelectedList) {
            return;
        }

        modalSelectedList.innerHTML = "";

        if (temporaryBenefits.length === 0) {
            const empty = document.createElement("p");

            empty.className = "benefit-modal-selected-empty";
            empty.textContent = "선택한 복지가 없습니다.";

            modalSelectedList.appendChild(empty);
        } else {
            temporaryBenefits.forEach(function (benefit) {
                const chip = document.createElement("span");
                const removeButton = document.createElement("button");

                chip.className = "benefit-modal-selected-chip";
                chip.append(document.createTextNode(benefit));

                removeButton.type = "button";
                removeButton.textContent = "×";
                removeButton.setAttribute("aria-label", benefit + " 선택 해제");

                removeButton.addEventListener("click", function () {
                    removeTemporaryBenefit(benefit);
                });

                chip.appendChild(removeButton);
                modalSelectedList.appendChild(chip);
            });
        }

        updateCounts();
    }

    function addTemporaryBenefit(benefit) {
        const duplicate = temporaryBenefits.some(function (item) {
            return item === benefit;
        });

        if (duplicate) {
            return;
        }

        if (temporaryBenefits.length >= MAX_BENEFITS) {
            showMessage("복지는 최대 " + MAX_BENEFITS + "개까지 선택할 수 있습니다.");

            synchronizeCheckboxes();
            return;
        }

        temporaryBenefits.push(benefit);

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedBenefits();
    }

    function removeTemporaryBenefit(benefit) {
        temporaryBenefits = temporaryBenefits.filter(function (item) {
            return item !== benefit;
        });

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedBenefits();
    }

    function addCustomBenefit() {
        if (!customInput) {
            return;
        }

        const value = customInput.value.trim();

        if (value.length === 0) {
            showMessage("추가할 복지 항목을 입력해주세요.");
            return;
        }

        addTemporaryBenefit(value);
        customInput.value = "";
    }

    function renderPageBenefits() {
        if (!pageChipList || !hiddenInputs) {
            return;
        }

        pageChipList.innerHTML = "";
        hiddenInputs.innerHTML = "";

        confirmedBenefits.forEach(function (benefit) {
            const chip = document.createElement("button");
            const name = document.createElement("span");
            const removeMark = document.createElement("span");
            const hiddenInput = document.createElement("input");

            chip.type = "button";
            chip.className = "company-benefit-chip";
            chip.dataset.benefitValue = benefit;
            chip.setAttribute("aria-label", benefit + " 삭제");

            name.textContent = benefit;
            removeMark.textContent = "×";
            removeMark.setAttribute("aria-hidden", "true");

            chip.appendChild(name);
            chip.appendChild(removeMark);

            chip.addEventListener("click", function () {
                confirmedBenefits = confirmedBenefits.filter(function (item) {
                    return item !== benefit;
                });

                renderPageBenefits();
            });

            pageChipList.appendChild(chip);

            hiddenInput.type = "hidden";
            hiddenInput.name = "benefits";
            hiddenInput.value = benefit;

            hiddenInputs.appendChild(hiddenInput);
        });

        const count = confirmedBenefits.length;

        if (pageEmpty) {
            pageEmpty.hidden = count !== 0;
        }

        if (pageCount) {
            pageCount.textContent = count + "/" + MAX_BENEFITS;
        }
    }

    function openModal() {
        previouslyFocusedElement = document.activeElement;

        temporaryBenefits = confirmedBenefits.slice();

        if (customInput) {
            customInput.value = "";
        }

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedBenefits();

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";

        clearMessage();
        previouslyFocusedElement?.focus();
    }

    optionCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            if (checkbox.checked) {
                addTemporaryBenefit(checkbox.value);
            } else {
                removeTemporaryBenefit(checkbox.value);
            }
        });
    });

    customAddButton?.addEventListener("click", addCustomBenefit);

    customInput?.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            event.preventDefault();
            addCustomBenefit();
        }
    });

    openButtons.forEach(function (button) {
        button.addEventListener("click", openModal);
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    cancelButton?.addEventListener("click", closeModal);

    confirmButton?.addEventListener("click", function () {
        confirmedBenefits = temporaryBenefits.slice();

        renderPageBenefits();
        closeModal();
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });

    renderPageBenefits();
});
