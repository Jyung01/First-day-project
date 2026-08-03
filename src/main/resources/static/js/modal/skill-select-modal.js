document.addEventListener("DOMContentLoaded", function () {
    const modal = document.querySelector("[data-skill-modal]");
    const openButtons = document.querySelectorAll("[data-skill-modal-open]");

    if (!modal || openButtons.length === 0) {
        return;
    }

    const closeButtons = modal.querySelectorAll("[data-skill-modal-close]");
    const cancelButton = modal.querySelector("[data-skill-modal-cancel]");
    const confirmButton = modal.querySelector("[data-skill-modal-confirm]");

    const categoryButtons = Array.from(modal.querySelectorAll("[data-skill-category]"));
    const categoryTitle = modal.querySelector("[data-skill-category-title]");
    const categoryDescription = modal.querySelector("[data-skill-category-description]");

    const searchInput = modal.querySelector("[data-skill-search]");
    const optionItems = Array.from(modal.querySelectorAll("[data-skill-option-item]"));
    const optionCheckboxes = Array.from(modal.querySelectorAll("[data-skill-option]"));
    const optionEmpty = modal.querySelector("[data-skill-option-empty]");

    const modalSelectedList = modal.querySelector("[data-skill-modal-selected-list]");
    const modalCount = modal.querySelector("[data-skill-modal-count]");
    const confirmCount = modal.querySelector("[data-skill-confirm-count]");
    const modalMessage = modal.querySelector("[data-skill-modal-message]");
    const modalMaximum = modal.querySelector("[data-skill-modal-maximum]");

    const pageChipList = document.querySelector("[data-skill-chip-list]");
    const pageEmpty = document.querySelector("[data-skill-empty]");
    const hiddenInputs = document.querySelector("[data-skill-hidden-inputs]");
    const pageCount = document.querySelector("[data-skill-selection-count]");
    const summaryCount = document.querySelector("[data-skill-summary-count]");

    const maximumSelection = Number(pageChipList?.dataset.skillMaximum || 10);
    const modalGuide = modal.querySelector(".skill-modal-header p");
    if (modalGuide && maximumSelection !== 10) {
        modalGuide.textContent = "공통 기술 DB에서 최대 " + maximumSelection + "개를 선택할 수 있습니다.";
    }
    Array.from(modalCount?.parentElement?.childNodes || []).forEach(function (node) {
        if (node.nodeType === Node.TEXT_NODE && node.textContent.includes("10") && maximumSelection !== 10) {
            node.textContent = node.textContent.replace("10", String(maximumSelection));
        }
    });
    if (modalMaximum) {
        modalMaximum.textContent = String(maximumSelection);
    }

    let confirmedSkills = readPageSkills();
    let temporarySkills = [];
    let activeCategory = "backend";
    let previouslyFocusedElement = null;

    const categoryNames = {
        backend: "백엔드 기술",
        frontend: "프론트엔드 기술",
        database: "데이터베이스 기술",
        infra: "인프라·클라우드 기술",
        "data-ai": "데이터·AI 기술",
        etc: "협업·기타 기술",
    };

    function readPageSkills() {
        return Array.from(pageChipList?.querySelectorAll("[data-skill-id]") ?? []).map(function (chip) {
            return {
                id: String(chip.dataset.skillId),
                name: chip.dataset.skillName,
            };
        });
    }

    function cloneSkills(skills) {
        return skills.map(function (skill) {
            return { ...skill };
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
        const count = temporarySkills.length;

        if (modalCount) {
            modalCount.textContent = String(count);
        }

        if (confirmCount) {
            confirmCount.textContent = String(count);
        }
    }

    function synchronizeCheckboxes() {
        optionCheckboxes.forEach(function (checkbox) {
            checkbox.checked = temporarySkills.some(function (skill) {
                return skill.id === checkbox.value;
            });
        });
    }

    function renderModalSelectedSkills() {
        if (!modalSelectedList) {
            return;
        }

        modalSelectedList.innerHTML = "";

        if (temporarySkills.length === 0) {
            const empty = document.createElement("p");

            empty.className = "skill-modal-selected-empty";
            empty.textContent = "선택한 기술이 없습니다.";

            modalSelectedList.appendChild(empty);
        } else {
            temporarySkills.forEach(function (skill) {
                const chip = document.createElement("span");
                const removeButton = document.createElement("button");

                chip.className = "skill-modal-selected-chip";
                chip.append(document.createTextNode(skill.name));

                removeButton.type = "button";
                removeButton.textContent = "×";
                removeButton.setAttribute("aria-label", skill.name + " 선택 해제");

                removeButton.addEventListener("click", function () {
                    removeTemporarySkill(skill.id);
                });

                chip.appendChild(removeButton);
                modalSelectedList.appendChild(chip);
            });
        }

        updateCounts();
    }

    function addTemporarySkill(skill) {
        const duplicate = temporarySkills.some(function (item) {
            return item.id === skill.id;
        });

        if (duplicate) {
            return;
        }

        if (temporarySkills.length >= maximumSelection) {
            showMessage("기술은 최대 " + maximumSelection + "개까지 선택할 수 있습니다.");

            synchronizeCheckboxes();
            return;
        }

        temporarySkills.push(skill);

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedSkills();
    }

    function removeTemporarySkill(skillId) {
        temporarySkills = temporarySkills.filter(function (skill) {
            return skill.id !== String(skillId);
        });

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedSkills();
    }

    function renderPageSkills() {
        if (!pageChipList || !hiddenInputs) {
            return;
        }

        pageChipList.innerHTML = "";
        hiddenInputs.innerHTML = "";

        confirmedSkills.forEach(function (skill) {
            const chip = document.createElement("button");
            const name = document.createElement("span");
            const removeMark = document.createElement("span");
            const hiddenInput = document.createElement("input");

            chip.type = "button";
            chip.className = "resume-skill-chip";
            chip.dataset.skillId = skill.id;
            chip.dataset.skillName = skill.name;
            chip.setAttribute("aria-label", skill.name + " 삭제");

            name.textContent = skill.name;
            removeMark.textContent = "×";
            removeMark.setAttribute("aria-hidden", "true");

            chip.appendChild(name);
            chip.appendChild(removeMark);

            chip.addEventListener("click", function () {
                confirmedSkills = confirmedSkills.filter(function (item) {
                    return item.id !== skill.id;
                });

                renderPageSkills();
            });

            pageChipList.appendChild(chip);

            hiddenInput.type = "hidden";
            hiddenInput.name = "skillIds";
            hiddenInput.value = skill.id;

            hiddenInputs.appendChild(hiddenInput);
        });

        const count = confirmedSkills.length;

        if (pageEmpty) {
            pageEmpty.hidden = count !== 0;
        }

        if (pageCount) {
            pageCount.textContent = count + "/" + maximumSelection;
        }

        if (summaryCount) {
            summaryCount.textContent = String(count);
        }
    }

    function filterOptions() {
        const keyword = searchInput?.value.trim().toLowerCase() ?? "";

        let visibleCount = 0;

        optionItems.forEach(function (item) {
            const categoryMatches = keyword.length > 0 || item.dataset.skillCategoryName === activeCategory;

            const skillName = item.dataset.skillSearchName?.toLowerCase() ?? "";

            const searchMatches = !keyword || skillName.includes(keyword);

            const visible = categoryMatches && searchMatches;

            item.hidden = !visible;

            if (visible) {
                visibleCount += 1;
            }
        });

        if (optionEmpty) {
            optionEmpty.hidden = visibleCount !== 0;
        }

        if (categoryTitle) {
            categoryTitle.textContent = keyword.length > 0 ? "검색 결과" : categoryNames[activeCategory];
        }

        if (categoryDescription) {
            categoryDescription.textContent =
                keyword.length > 0
                    ? `"${searchInput.value.trim()}" 검색 결과입니다.`
                    : "클릭하면 위 선택 영역에 추가됩니다.";
        }
    }

    function activateCategory(category) {
        activeCategory = category;

        categoryButtons.forEach(function (button) {
            button.classList.toggle("is-active", button.dataset.skillCategory === category);
        });

        if (searchInput) {
            searchInput.value = "";
        }

        filterOptions();
    }

    function openModal() {
        previouslyFocusedElement = document.activeElement;

        temporarySkills = cloneSkills(confirmedSkills);

        clearMessage();
        synchronizeCheckboxes();
        renderModalSelectedSkills();
        activateCategory("backend");

        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";

        window.setTimeout(function () {
            searchInput?.focus();
        }, 0);
    }

    function closeModal() {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
        document.body.style.overflow = "";

        clearMessage();
        previouslyFocusedElement?.focus();
    }

    categoryButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            activateCategory(button.dataset.skillCategory);
        });
    });

    optionCheckboxes.forEach(function (checkbox) {
        checkbox.addEventListener("change", function () {
            const skill = {
                id: checkbox.value,
                name: checkbox.dataset.skillName,
            };

            if (checkbox.checked) {
                addTemporarySkill(skill);
            } else {
                removeTemporarySkill(skill.id);
            }
        });
    });

    searchInput?.addEventListener("input", filterOptions);

    openButtons.forEach(function (button) {
        button.addEventListener("click", openModal);
    });

    closeButtons.forEach(function (button) {
        button.addEventListener("click", closeModal);
    });

    cancelButton?.addEventListener("click", closeModal);

    confirmButton?.addEventListener("click", function () {
        confirmedSkills = cloneSkills(temporarySkills);

        renderPageSkills();
        closeModal();
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && modal.classList.contains("is-open")) {
            closeModal();
        }
    });

    renderPageSkills();
    activateCategory("backend");
});
