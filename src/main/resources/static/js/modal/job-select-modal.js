document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-job-modal]");
    const openButtons = document.querySelectorAll("[data-job-modal-open]");

    if (!modal || openButtons.length === 0) {
        return;
    }

    const closeButtons = modal.querySelectorAll("[data-job-modal-close]");
    const cancelButton = modal.querySelector("[data-job-modal-cancel]");
    const confirmButton = modal.querySelector("[data-job-modal-confirm]");

    const primaryButtons = modal.querySelectorAll("[data-job-primary]");
    const secondaryLists = modal.querySelectorAll(
        "[data-job-secondary-list]"
    );
    const secondaryTitle = modal.querySelector(
        "[data-job-secondary-title]"
    );

    const optionCheckboxes = Array.from(
        modal.querySelectorAll("[data-job-option]")
    );

    const modalCount = modal.querySelector("[data-job-modal-count]");
    const modalSelectedList = modal.querySelector(
        "[data-job-modal-selected-list]"
    );
    const modalMessage = modal.querySelector("[data-job-modal-message]");

    const pageSelectedList = document.querySelector(
        "[data-job-selected-list]"
    );
    const pageSelectedEmpty = document.querySelector(
        "[data-job-selected-empty]"
    );
    const hiddenInputsContainer = document.querySelector(
        "[data-job-hidden-inputs]"
    );
    const pageSelectionCount = document.querySelector(
        "[data-job-selection-count]"
    );
    const pageJobMessage = document.querySelector("[data-job-message]");

    const maximumSelection = 3;

    let confirmedJobs = [];
    let temporaryJobs = [];

    function findOption(jobId) {
        return optionCheckboxes.find(function (checkbox) {
            return checkbox.value === String(jobId);
        });
    }

    function getJobFromCheckbox(checkbox) {
        return {
            id: checkbox.value,
            name: checkbox.dataset.jobName
        };
    }

    function showModalMessage(message) {
        if (!modalMessage) {
            return;
        }

        modalMessage.textContent = message;
        modalMessage.classList.add("is-visible");
    }

    function clearModalMessage() {
        if (!modalMessage) {
            return;
        }

        modalMessage.textContent = "";
        modalMessage.classList.remove("is-visible");
    }

    function renderModalSelectedJobs() {
        if (!modalSelectedList) {
            return;
        }

        modalSelectedList.innerHTML = "";

        if (temporaryJobs.length === 0) {
            const empty = document.createElement("span");

            empty.className = "job-modal-selected-empty";
            empty.textContent = "선택된 직무가 없습니다.";

            modalSelectedList.appendChild(empty);
        } else {
            temporaryJobs.forEach(function (job) {
                const chip = document.createElement("span");
                const removeButton = document.createElement("button");

                chip.className = "job-modal-chip";
                chip.textContent = job.name;

                removeButton.type = "button";
                removeButton.textContent = "×";
                removeButton.setAttribute(
                    "aria-label",
                    job.name + " 선택 해제"
                );

                removeButton.addEventListener("click", function () {
                    removeTemporaryJob(job.id);
                });

                chip.appendChild(removeButton);
                modalSelectedList.appendChild(chip);
            });
        }

        if (modalCount) {
            modalCount.textContent = String(temporaryJobs.length);
        }
    }

    function synchronizeCheckboxes() {
        optionCheckboxes.forEach(function (checkbox) {
            checkbox.checked = temporaryJobs.some(function (job) {
                return job.id === checkbox.value;
            });
        });
    }

    function addTemporaryJob(job) {
        const alreadySelected = temporaryJobs.some(function (item) {
            return item.id === job.id;
        });

        if (alreadySelected) {
            return;
        }

        if (temporaryJobs.length >= maximumSelection) {
            showModalMessage(
                "희망 직무는 최대 3개까지 선택할 수 있습니다."
            );

            const checkbox = findOption(job.id);

            if (checkbox) {
                checkbox.checked = false;
            }

            return;
        }

        temporaryJobs.push(job);
        clearModalMessage();
        renderModalSelectedJobs();
    }

    function removeTemporaryJob(jobId) {
        temporaryJobs = temporaryJobs.filter(function (job) {
            return job.id !== String(jobId);
        });

        clearModalMessage();
        synchronizeCheckboxes();
        renderModalSelectedJobs();
    }

    function renderPageSelectedJobs() {
        if (!pageSelectedList || !hiddenInputsContainer) {
            return;
        }

        pageSelectedList.innerHTML = "";
        hiddenInputsContainer.innerHTML = "";

        if (confirmedJobs.length === 0) {
            const empty = document.createElement("p");

            empty.className = "job-selected-empty";
            empty.dataset.jobSelectedEmpty = "";
            empty.textContent = "선택된 희망 직무가 없습니다.";

            pageSelectedList.appendChild(empty);
        } else {
            confirmedJobs.forEach(function (job) {
                const chip = document.createElement("span");
                const removeButton = document.createElement("button");
                const hiddenInput = document.createElement("input");

                chip.className = "job-selected-chip";
                chip.textContent = job.name;

                removeButton.type = "button";
                removeButton.textContent = "×";
                removeButton.setAttribute(
                    "aria-label",
                    job.name + " 삭제"
                );

                removeButton.addEventListener("click", function () {
                    confirmedJobs = confirmedJobs.filter(function (item) {
                        return item.id !== job.id;
                    });

                    renderPageSelectedJobs();
                });

                chip.appendChild(removeButton);
                pageSelectedList.appendChild(chip);

                hiddenInput.type = "hidden";
                hiddenInput.name = "desiredJobIds";
                hiddenInput.value = job.id;

                hiddenInputsContainer.appendChild(hiddenInput);
            });
        }

        if (pageSelectionCount) {
            pageSelectionCount.textContent =
                confirmedJobs.length + "/" + maximumSelection;
        }

        if (pageJobMessage) {
            pageJobMessage.textContent = "";
            pageJobMessage.classList.remove(
                "is-visible",
                "is-success"
            );
        }
    }

    function openModal() {
        temporaryJobs = confirmedJobs.map(function (job) {
            return { ...job };
        });

        clearModalMessage();
        synchronizeCheckboxes();
        renderModalSelectedJobs();

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");

        document.body.style.overflow = "hidden";
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");

        document.body.style.overflow = "";
    }

    primaryButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const selectedCategory = button.dataset.jobPrimary;

            primaryButtons.forEach(function (item) {
                item.classList.toggle(
                    "is-active",
                    item === button
                );
            });

            secondaryLists.forEach(function (list) {
                list.classList.toggle(
                    "is-active",
                    list.dataset.jobSecondaryList === selectedCategory
                );
            });

            if (secondaryTitle) {
                secondaryTitle.textContent = button.textContent.trim();
            }
        });
    });

    optionCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            const job = getJobFromCheckbox(checkbox);

            if (checkbox.checked) {
                addTemporaryJob(job);
            } else {
                removeTemporaryJob(job.id);
            }
        });
    });

    openButtons.forEach(function (button) {
        button.addEventListener("click", openModal);
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    cancelButton?.addEventListener("click", closeModal);

    confirmButton?.addEventListener("click", function () {
        confirmedJobs = temporaryJobs.map(function (job) {
            return { ...job };
        });

        renderPageSelectedJobs();
        closeModal();
    });

    document.addEventListener("keydown", function (event) {
        if (
            event.key === "Escape" &&
            modal.classList.contains("is-open")
        ) {
            closeModal();
        }
    });

    renderPageSelectedJobs();
});