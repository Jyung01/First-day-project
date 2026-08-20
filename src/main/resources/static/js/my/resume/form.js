document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-resume-form]");

    if (!form) {
        return;
    }

    const tabButtons = Array.from(document.querySelectorAll("[data-resume-tab]"));
    const panels = Array.from(document.querySelectorAll("[data-resume-panel]"));

    const educationList = form.querySelector("[data-education-list]");
    const careerList = form.querySelector("[data-career-list]");
    const projectList = form.querySelector("[data-project-list]");
    const skillChipList = form.querySelector("[data-skill-chip-list]");

    const educationTemplate = document.getElementById("educationItemTemplate");
    const careerTemplate = document.getElementById("careerItemTemplate");
    const projectTemplate = document.getElementById("projectItemTemplate");

    const addEducationButton = form.querySelector("[data-add-education]");
    const addCareerButton = form.querySelector("[data-add-career]");
    const addProjectButton = form.querySelector("[data-add-project]");

    const educationSummaryCount = form.querySelector("[data-education-summary-count]");
    const skillSummaryCount = form.querySelector("[data-skill-summary-count]");
    const projectSummaryCount = form.querySelector("[data-project-summary-count]");

    const cancelLink = form.querySelector("[data-resume-cancel]");

    /* =====================================================
       기술 선택 모달 - 실제 DB 기술 목록 채우기
       (skill-select-modal.html 자체 옵션은 화면 목업용 더미 값이라
        resume_skills.skill_id 외래키와 맞지 않는다. 여기서 실제
        DB 기술 목록(data-resume-skill-source)으로 모달 내용을 교체한다.)
    ===================================================== */

    function renderDatabaseSkills() {
        const sourceItems = Array.from(
            document.querySelectorAll("[data-resume-skill-source] [data-skill-id]")
        );
        const modal = document.querySelector("[data-skill-modal]");

        if (sourceItems.length === 0 || !modal) {
            return;
        }

        const categoryList = modal.querySelector(".skill-category-list");
        const optionList = modal.querySelector("[data-skill-option-list]");
        const groups = sourceItems.filter(function (item) {
            return item.dataset.depth === "1";
        });
        const skills = sourceItems.filter(function (item) {
            return item.dataset.depth === "2";
        });

        if (!categoryList || !optionList || groups.length === 0) {
            return;
        }

        categoryList.querySelectorAll("[data-skill-category]").forEach(function (button) {
            button.remove();
        });
        optionList.replaceChildren();

        groups.forEach(function (group, index) {
            const categoryKey = "skill-group-" + group.dataset.skillId;
            const button = document.createElement("button");
            const name = document.createElement("span");
            const arrow = document.createElement("span");

            button.type = "button";
            button.className = "skill-category-button";
            button.dataset.skillCategory = categoryKey;

            if (index === 0) {
                button.classList.add("is-active");
            }

            name.textContent = group.dataset.skillName;
            arrow.textContent = "›";
            arrow.setAttribute("aria-hidden", "true");
            button.append(name, arrow);
            categoryList.appendChild(button);

            skills
                .filter(function (skill) {
                    return skill.dataset.parentId === group.dataset.skillId;
                })
                .forEach(function (skill) {
                    const label = document.createElement("label");
                    const checkbox = document.createElement("input");
                    const skillName = document.createElement("span");

                    label.className = "skill-option";
                    label.dataset.skillOptionItem = "";
                    label.dataset.skillCategoryName = categoryKey;
                    label.dataset.skillSearchName = skill.dataset.skillName.toLowerCase();
                    label.hidden = index !== 0;

                    checkbox.type = "checkbox";
                    checkbox.value = skill.dataset.skillId;
                    checkbox.dataset.skillOption = "";
                    checkbox.dataset.skillName = skill.dataset.skillName;
                    skillName.textContent = skill.dataset.skillName;

                    label.append(checkbox, skillName);
                    optionList.appendChild(label);
                });
        });

        const categoryTitle = modal.querySelector("[data-skill-category-title]");

        if (categoryTitle) {
            categoryTitle.textContent = groups[0].dataset.skillName;
        }
    }

    renderDatabaseSkills();

    /* =====================================================
       탭
    ===================================================== */

    function activateTab(tabName, focusTab = false) {
        tabButtons.forEach(function (button) {
            const active = button.dataset.resumeTab === tabName;

            button.classList.toggle("is-active", active);
            button.setAttribute("aria-selected", String(active));

            if (active && focusTab) {
                button.focus();
            }
        });

        panels.forEach(function (panel) {
            const active = panel.dataset.resumePanel === tabName;

            panel.classList.toggle("is-active", active);
            panel.hidden = !active;
        });
    }

    tabButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            activateTab(button.dataset.resumeTab);
        });
    });

    document.querySelectorAll("[data-resume-tab-move]").forEach(function (button) {
        button.addEventListener("click", function () {
            activateTab(button.dataset.resumeTabMove, true);
        });
    });

    /* =====================================================
       반복 항목 이름 재정렬
    ===================================================== */

    function reindexItems(config) {
        const items = Array.from(config.list.querySelectorAll(config.itemSelector));

        items.forEach(function (item, index) {
            const title = item.querySelector(config.titleSelector);

            if (title) {
                title.textContent = config.titlePrefix + " " + (index + 1);
            }

            item.querySelectorAll(`[${config.fieldAttribute}]`).forEach(function (field) {
                const fieldName = field.getAttribute(config.fieldAttribute);

                field.name = config.namePrefix + "[" + index + "]." + fieldName;
            });
        });

        updateSummaryCounts();
    }

    function reindexEducations() {
        reindexItems({
            list: educationList,
            itemSelector: "[data-education-item]",
            titleSelector: "[data-education-title]",
            titlePrefix: "학력",
            fieldAttribute: "data-education-field",
            namePrefix: "educations",
        });
    }

    function reindexCareers() {
        reindexItems({
            list: careerList,
            itemSelector: "[data-career-item]",
            titleSelector: "[data-career-title]",
            titlePrefix: "경력",
            fieldAttribute: "data-career-field",
            namePrefix: "careers",
        });
    }

    function reindexProjects() {
        reindexItems({
            list: projectList,
            itemSelector: "[data-project-item]",
            titleSelector: "[data-project-title]",
            titlePrefix: "프로젝트",
            fieldAttribute: "data-project-field",
            namePrefix: "projects",
        });
    }

    function updateSummaryCounts() {
        const educationCount = educationList?.querySelectorAll("[data-education-item]").length ?? 0;

        const projectCount = projectList?.querySelectorAll("[data-project-item]").length ?? 0;

        const skillCount = skillChipList?.querySelectorAll("[data-skill-id]").length ?? 0;

        if (educationSummaryCount) {
            educationSummaryCount.textContent = String(educationCount);
        }

        if (projectSummaryCount) {
            projectSummaryCount.textContent = String(projectCount);
        }

        if (skillSummaryCount) {
            skillSummaryCount.textContent = String(skillCount);
        }
    }

    /* =====================================================
       반복 항목 추가
    ===================================================== */

    function appendTemplate(template, list, callback) {
        if (!template || !list) {
            return;
        }

        const fragment = template.content.cloneNode(true);
        const item = fragment.firstElementChild;

        list.appendChild(fragment);

        callback();

        item?.scrollIntoView({
            behavior: "smooth",
            block: "center",
        });

        const firstInput = item?.querySelector("input, select, textarea");

        window.setTimeout(function () {
            firstInput?.focus();
        }, 250);
    }

    addEducationButton?.addEventListener("click", function () {
        appendTemplate(educationTemplate, educationList, reindexEducations);
    });

    addCareerButton?.addEventListener("click", function () {
        appendTemplate(careerTemplate, careerList, reindexCareers);
    });

    addProjectButton?.addEventListener("click", function () {
        appendTemplate(projectTemplate, projectList, reindexProjects);
    });

    /* =====================================================
       반복 항목 삭제
    ===================================================== */

    function removeRepeatItem(button, options) {
        const item = button.closest(options.itemSelector);

        if (!item) {
            return;
        }

        const total = options.list.querySelectorAll(options.itemSelector).length;

        /*
         * 학력은 최소 1개를 유지한다.
         * 경력과 프로젝트는 0개까지 허용한다.
         */
        if (options.minimum > 0 && total <= options.minimum) {
            showConfirmModal({
                iconClass: "info",
                iconHtml: "i",
                title: options.minimumTitle,
                message: options.minimumMessage,
                // 안내만 하는 모달이라 왼쪽 버튼을 숨긴다.
                // leftText만 비우면 버튼은 그대로 남아 빈 입력창처럼 보인다.
                leftVisible: false,
                rightText: "확인",
                rightClass: "btn-primary",
            });

            return;
        }

        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: options.confirmTitle,
            message: options.confirmMessage,
            leftText: "취소",
            rightText: "삭제",
            leftClass: "btn-outline",
            rightClass: "btn-danger",

            onRight: function () {
                item.remove();
                options.reindex();
            },
        });
    }

    form.addEventListener("click", function (event) {
        const educationRemove = event.target.closest("[data-remove-education]");

        if (educationRemove) {
            removeRepeatItem(educationRemove, {
                list: educationList,
                itemSelector: "[data-education-item]",
                minimum: 1,
                minimumTitle: "학력은 한 건 이상 필요합니다",
                minimumMessage: "이력서에는 최소 한 건의 학력 정보를 입력해주세요.",
                confirmTitle: "학력을 삭제할까요?",
                confirmMessage: "입력한 학력 정보가 삭제됩니다.",
                reindex: reindexEducations,
            });

            return;
        }

        const careerRemove = event.target.closest("[data-remove-career]");

        if (careerRemove) {
            removeRepeatItem(careerRemove, {
                list: careerList,
                itemSelector: "[data-career-item]",
                minimum: 0,
                confirmTitle: "경력을 삭제할까요?",
                confirmMessage: "입력한 경력 정보가 삭제됩니다.",
                reindex: reindexCareers,
            });

            return;
        }

        const projectRemove = event.target.closest("[data-remove-project]");

        if (projectRemove) {
            removeRepeatItem(projectRemove, {
                list: projectList,
                itemSelector: "[data-project-item]",
                minimum: 0,
                confirmTitle: "프로젝트를 삭제할까요?",
                confirmMessage: "입력한 프로젝트 정보가 삭제됩니다.",
                reindex: reindexProjects,
            });
        }
    });

    /* =====================================================
       기술 선택
    ===================================================== */

    document.addEventListener("click", function (event) {
        const insideSelector = event.target.closest(".resume-skill-selector");

        if (!insideSelector && skillSuggestions) {
            skillSuggestions.hidden = true;
        }
    });

    /* =====================================================
       글자 수
    ===================================================== */

    function initializeTextCounters() {
        document.querySelectorAll("[data-text-count]").forEach(function (counter) {
            const targetId = counter.dataset.textCount;
            const target = document.getElementById(targetId);

            if (!target) {
                return;
            }

            function update() {
                counter.textContent = String(target.value.length);
            }

            target.addEventListener("input", update);
            update();
        });
    }

    /* =====================================================
       검증
    ===================================================== */

    function clearValidationState() {
        form.querySelectorAll(".is-error").forEach(function (element) {
            element.classList.remove("is-error");
        });

        form.querySelectorAll(".resume-field-message.is-visible").forEach(function (message) {
            /*
             * 기술 선택 메시지는 별도 제어하므로 제외
             */
            if (!message.matches("[data-skill-message]")) {
                message.textContent = "";
                message.classList.remove("is-visible");
            }
        });
    }

    function showFieldError(field, message) {
        field.classList.add("is-error");

        const fieldId = field.id;

        if (fieldId) {
            const messageElement = form.querySelector(`[data-field-message="${fieldId}"]`);

            if (messageElement) {
                messageElement.textContent = message;
                messageElement.classList.add("is-visible");
            }
        }
    }

    function validateForm() {
        clearValidationState();

        const requiredFields = Array.from(form.querySelectorAll("[data-required-field]"));

        const invalidField = requiredFields.find(function (field) {
            return !String(field.value).trim();
        });

        if (!invalidField) {
            return true;
        }

        const targetTab = invalidField.dataset.requiredTab || "basic";

        activateTab(targetTab);

        showFieldError(invalidField, "필수 항목을 입력해주세요.");

        window.setTimeout(function () {
            invalidField.focus();
            invalidField.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }, 0);

        return false;
    }

    /* =====================================================
       취소 확인
    ===================================================== */

    cancelLink?.addEventListener("click", function (event) {
        event.preventDefault();

        const targetUrl = cancelLink.getAttribute("href");

        showConfirmModal({
            iconClass: "danger",
            iconHtml: "!",
            title: "이력서 작성을 취소할까요?",
            message: "저장하지 않은 내용은 사라집니다.\n이력서 관리 화면으로 돌아갈까요?",
            leftText: "계속 작성",
            rightText: "작성 취소",
            leftClass: "btn-outline",
            rightClass: "btn-danger",

            onRight: function () {
                window.location.href = targetUrl;
            },
        });
    });

    /* =====================================================
       제출
    ===================================================== */

    form.addEventListener("submit", function (event) {
        if (!validateForm()) {
            event.preventDefault();
            return;
        }

        reindexEducations();
        reindexCareers();
        reindexProjects();
    });

    /* =====================================================
       초기화
    ===================================================== */

    reindexEducations();
    reindexCareers();
    reindexProjects();
    initializeTextCounters();
    activateTab("basic");
});
