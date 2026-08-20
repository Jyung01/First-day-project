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

    /**
     * 필드에 딸린 안내 문구 자리를 찾는다.
     *
     * 기본정보처럼 id가 있는 필드는 미리 만들어 둔 자리를 쓰고,
     * 학력·경력처럼 행이 늘어나는 입력은 id가 없어 자리가 없으므로 그때 만들어 붙인다.
     * 예전에는 후자에 문구가 아예 안 떠서 테두리만 붉어졌다.
     */
    function resolveMessageElement(field) {
        if (field.id) {
            const existing = form.querySelector(`[data-field-message="${field.id}"]`);

            if (existing) {
                return existing;
            }
        }

        const wrapper = field.closest(".resume-field") || field.parentElement;

        if (!wrapper) {
            return null;
        }

        let created = wrapper.querySelector(".resume-field-message");

        if (!created) {
            created = document.createElement("p");
            created.className = "resume-field-message";
            wrapper.appendChild(created);
        }

        return created;
    }

    function showFieldError(field, message) {
        field.classList.add("is-error");

        const messageElement = resolveMessageElement(field);

        if (messageElement) {
            messageElement.textContent = message;
            messageElement.classList.add("is-visible");
        }
    }

    function clearFieldError(field) {
        field.classList.remove("is-error");

        const messageElement = resolveMessageElement(field);

        if (messageElement && !messageElement.matches("[data-skill-message]")) {
            messageElement.textContent = "";
            messageElement.classList.remove("is-visible");
        }
    }

    /* =====================================================
       연/월 선택

       날짜는 <input type="month"> 대신 연도·월 선택 상자를 쓴다.
       HTML 규격상 month 입력의 연도는 "4자리 이상"이라 131212년도 유효한 값이고,
       maxlength·pattern이 적용되지 않아 자릿수를 막을 방법이 없기 때문이다.

       서버로 보내는 값은 그대로 "YYYY-MM"이다. 두 상자는 화면용이고,
       실제로 제출되는 것은 같은 그룹 안의 hidden input이다.
       이름(name)도 hidden이 그대로 들고 있어 reindex와 서버 바인딩이 바뀌지 않는다.
    ===================================================== */

    /** 두 상자의 선택을 hidden input의 "YYYY-MM"으로 합친다. */
    function syncDateGroup(group) {
        const year = group.querySelector("[data-date-year]");
        const month = group.querySelector("[data-date-month]");
        const target = group.querySelector("[data-date-value]");

        if (!year || !month || !target) {
            return;
        }

        // 한쪽만 고른 상태는 날짜로 볼 수 없다. 비워서 "입력 안 함"으로 둔다.
        target.value = year.value && month.value ? `${year.value}-${month.value}` : "";
    }

    /** hidden input에 들어 있는 값으로 두 상자를 맞춘다 (수정 화면·검증 실패 후 재표시). */
    function fillDateGroup(group) {
        const year = group.querySelector("[data-date-year]");
        const month = group.querySelector("[data-date-month]");
        const source = group.querySelector("[data-date-value]");

        if (!year || !month || !source || !source.value) {
            return;
        }

        const [yearPart, monthPart] = String(source.value).split("-");

        year.value = yearPart || "";
        month.value = monthPart || "";

        // 목록에 없는 연도(예전에 저장된 이상값)면 선택이 안 된다. 그때는 값을 비워 다시 고르게 한다.
        if (!year.value || !month.value) {
            year.value = "";
            month.value = "";
            syncDateGroup(group);
        }
    }

    form.querySelectorAll("[data-date-group]").forEach(fillDateGroup);

    form.addEventListener("change", function (event) {
        if (!event.target.matches?.("[data-date-year], [data-date-month]")) {
            return;
        }

        syncDateGroup(event.target.closest("[data-date-group]"));
    });

    /* =====================================================
       숫자 범위 제한

       폼에 novalidate가 걸려 있어 min/max 속성이 브라우저에서 동작하지 않는다.
       같은 규칙을 직접 구현해 칸을 벗어날 때 경계값으로 되돌린다.
       어디까지나 편의 기능이다. 실제 방어는 서버(ResumeDto의 Bean Validation)가 한다.
    ===================================================== */

    const RANGE_SELECTOR = 'input[type="number"]';

    /** 필드의 min/max 속성에서 허용 범위를 읽는다. */
    function resolveLimits(field) {
        const min = field.getAttribute("min");
        const max = field.getAttribute("max");

        return {
            min: min === null ? -Infinity : Number(min),
            max: max === null ? Infinity : Number(max),
            value: Number(field.value),
            minText: min,
            maxText: max,
            message: min !== null && max !== null
                ? `${min}에서 ${max} 사이로 입력해주세요.`
                : "값을 다시 확인해주세요.",
        };
    }

    /** 범위를 벗어났으면 사유를, 아니면 null을 돌려준다. */
    function checkRange(field) {
        if (!String(field.value).trim()) {
            return null;
        }

        const limits = resolveLimits(field);

        if (!Number.isFinite(limits.value)) {
            return { field: field, message: "형식에 맞게 입력해주세요.", limits: limits };
        }

        if (limits.value < limits.min || limits.value > limits.max) {
            return { field: field, message: limits.message, limits: limits };
        }

        return null;
    }

    /** 범위를 벗어난 값을 가까운 경계값으로 되돌린다. */
    function clampField(field) {
        const problem = checkRange(field);

        if (!problem) {
            return false;
        }

        const limits = problem.limits;

        if (!Number.isFinite(limits.value)) {
            field.value = "";
            return true;
        }

        field.value = limits.value < limits.min ? limits.minText : limits.maxText;
        return true;
    }

    function findOutOfRangeField() {
        for (const field of form.querySelectorAll(RANGE_SELECTOR)) {
            const problem = checkRange(field);

            if (problem) {
                return problem;
            }
        }

        return null;
    }

    /*
     * 행이 동적으로 늘어나므로 폼에 위임해서 듣는다.
     *
     * 검사 시점은 focusout(칸을 완전히 벗어날 때) 하나뿐이다.
     * 입력 도중에 값을 고치면 "12"를 치려는데 "9.99"로 바뀌는 식으로 사용자와 싸우게 된다.
     */
    form.addEventListener("focusout", function (event) {
        const field = event.target;

        if (!field.matches?.(RANGE_SELECTOR)) {
            return;
        }

        if (clampField(field)) {
            showFieldError(field, "입력할 수 있는 범위로 조정했습니다.");
        } else {
            clearFieldError(field);
        }
    });

    /** 필드가 속한 탭을 찾는다. 없으면 기본정보 탭으로 본다. */
    function resolveTab(field) {
        if (field.dataset.requiredTab) {
            return field.dataset.requiredTab;
        }

        const panel = field.closest("[data-resume-panel]");

        return panel ? panel.dataset.resumePanel : "basic";
    }

    /** 문제가 된 필드로 화면을 이동시키고 사유를 알린다. */
    function focusInvalidField(field, message) {
        activateTab(resolveTab(field));
        showFieldError(field, message);

        window.setTimeout(function () {
            field.focus();
            field.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }, 0);
    }

    function validateForm() {
        clearValidationState();

        const requiredFields = Array.from(form.querySelectorAll("[data-required-field]"));

        const emptyField = requiredFields.find(function (field) {
            return !String(field.value).trim();
        });

        if (emptyField) {
            focusInvalidField(emptyField, "필수 항목을 입력해주세요.");
            return false;
        }

        const outOfRange = findOutOfRangeField();

        if (outOfRange) {
            focusInvalidField(outOfRange.field, outOfRange.message);
            return false;
        }

        return true;
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
