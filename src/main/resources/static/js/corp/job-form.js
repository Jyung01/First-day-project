document.addEventListener("DOMContentLoaded", function () {
  function renderDatabaseSkills() {
    const sourceItems = Array.from(
      document.querySelectorAll("[data-job-skill-source] [data-skill-id]"),
    );
    const modal = document.querySelector("[data-skill-modal]");

    if (sourceItems.length === 0 || !modal) return;

    const categoryList = modal.querySelector(".skill-category-list");
    const optionList = modal.querySelector("[data-skill-option-list]");
    const groups = sourceItems.filter(function (item) {
      return item.dataset.depth === "1";
    });
    const skills = sourceItems.filter(function (item) {
      return item.dataset.depth === "2";
    });

    if (!categoryList || !optionList || groups.length === 0) return;

    categoryList
      .querySelectorAll("[data-skill-category]")
      .forEach(function (button) {
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

    const categoryTitle = modal.querySelector(
      "[data-skill-category-title]",
    );
    if (categoryTitle) {
      categoryTitle.textContent = groups[0].dataset.skillName;
    }
  }

  function restoreSelectedSkills() {
    const chipList = document.querySelector("[data-skill-chip-list]");
    const hiddenInputs = Array.from(
      document.querySelectorAll(
        "[data-skill-hidden-inputs] input[name='skillIds']",
      ),
    );

    if (!chipList || hiddenInputs.length === 0) return;

    const sourceItems = Array.from(
      document.querySelectorAll("[data-job-skill-source] [data-skill-id]"),
    );

    hiddenInputs.forEach(function (input) {
      const source = sourceItems.find(function (item) {
        return item.dataset.skillId === input.value;
      });

      if (!source) return;

      const chip = document.createElement("span");
      chip.className = "skill-chip";
      chip.dataset.skillId = source.dataset.skillId;
      chip.dataset.skillName = source.dataset.skillName;
      chip.textContent = source.dataset.skillName;
      chipList.appendChild(chip);
    });

    const empty = chipList.querySelector("[data-skill-empty]");
    if (empty) {
      empty.hidden = true;
    }

    const count = document.querySelector("[data-skill-selection-count]");
    if (count) {
      count.textContent = hiddenInputs.length + "/5";
    }
  }

  renderDatabaseSkills();
  restoreSelectedSkills();

  const form = document.querySelector("#jobForm");
  let reviewFormChanged = true;
  const primaryJobCategory = document.querySelector("#primaryJobCategory");
  const jobCategory = document.querySelector("#category");
  const jobCategoryOptions = jobCategory
    ? Array.from(jobCategory.querySelectorAll("[data-parent-category]"))
      .map(function (option) {
        return option.cloneNode(true);
      })
    : [];
  const careerType = document.querySelector("#careerType");
  const minExperience = document.querySelector("#minExperienceYears");
  const maxExperience = document.querySelector("#maxExperienceYears");
  const experienceHelp = document.querySelector("[data-experience-help]");
  const salaryText = document.querySelector("#salaryText");
  const salaryMin = document.querySelector("#salaryMin");
  const salaryMax = document.querySelector("#salaryMax");
  const salaryHelp = document.querySelector("[data-salary-help]");
  const address = document.querySelector("#workAddress");
  const workRegion = document.querySelector("#workRegion");
  const applyStartDate = document.querySelector("#applyStartDate");
  const applyEndDate = document.querySelector("#deadline");
  const applicationPeriodHelp = document.querySelector(
    "[data-application-period-help]",
  );

  function updateJobCategoryOptions() {
    if (!primaryJobCategory || !jobCategory) return;

    const parentId = primaryJobCategory.value;
    const placeholder = document.createElement("option");

    placeholder.value = "";
    placeholder.textContent = parentId
      ? "2차 카테고리를 선택하세요"
      : "먼저 1차 카테고리를 선택하세요";

    jobCategory.replaceChildren(placeholder);

    jobCategoryOptions
      .filter(function (option) {
        return option.dataset.parentCategory === parentId;
      })
      .forEach(function (option) {
        jobCategory.appendChild(option.cloneNode(true));
      });

    jobCategory.disabled = !parentId;
  }

  function restoreJobCategorySelection() {
    if (!primaryJobCategory || !jobCategory) return;

    const selectedCategoryId = jobCategory.dataset.selectedCategory;
    if (!selectedCategoryId) return;

    const selectedOption = jobCategoryOptions.find(function (option) {
      return option.value === selectedCategoryId;
    });

    if (!selectedOption) return;

    primaryJobCategory.value = selectedOption.dataset.parentCategory;
    updateJobCategoryOptions();
    jobCategory.value = selectedCategoryId;
  }

  function toggleRange(inputs, enabled) {
    inputs.forEach(function (input) {
      if (!input) return;

      input.disabled = !enabled;
      input.required = false;

      if (!enabled) {
        input.value = "";
      }
    });
  }

  function updateExperienceFields() {
    const enabled = careerType?.value === "경력";

    toggleRange([minExperience, maxExperience], enabled);

    if (experienceHelp) {
      experienceHelp.textContent = enabled
        ? "최소·최대 중 하나만 입력하면 이상·이하로 표시됩니다."
        : "경력을 선택하면 연수를 입력할 수 있습니다.";
    }
  }

  function updateSalaryFields() {
    const enabled = salaryText?.value === "연봉";

    toggleRange([salaryMin, salaryMax], enabled);

    if (salaryHelp) {
      salaryHelp.textContent = enabled
        ? "최소·최대 중 하나만 입력하면 이상·이하로 표시됩니다."
        : "연봉을 선택하면 금액을 입력할 수 있습니다.";
    }
  }

  function updateWorkRegion() {
    if (!address || !workRegion) return;

    const regionParts = address.value.trim().split(/\s+/).slice(0, 2);
    workRegion.value = regionParts.join(" ");
  }

  function validateRange(
    minInput,
    maxInput,
    help,
    emptyMessage,
    rangeMessage,
  ) {
    if (!minInput || !maxInput || minInput.disabled || maxInput.disabled) {
      return true;
    }

    const hasMinimum = minInput.value !== "";
    const hasMaximum = maxInput.value !== "";

    if (!hasMinimum && !hasMaximum) {
      help?.classList.add("is-error");

      if (help) {
        help.textContent = emptyMessage;
      }

      minInput.focus();
      return false;
    }

    if (
      !hasMinimum ||
      !hasMaximum ||
      Number(minInput.value) <= Number(maxInput.value)
    ) {
      help?.classList.remove("is-error");
      return true;
    }

    help?.classList.add("is-error");

    if (help) {
      help.textContent = rangeMessage;
    }

    minInput.focus();
    return false;
  }

  function getTodayValue() {
    const today = new Date();
    const offset = today.getTimezoneOffset() * 60 * 1000;

    return new Date(today.getTime() - offset).toISOString().slice(0, 10);
  }

  function setApplicationPeriodMessage(message, error) {
    if (!applicationPeriodHelp) return;

    applicationPeriodHelp.textContent = message;
    applicationPeriodHelp.hidden = !message;
    applicationPeriodHelp.classList.toggle("is-error", error);
  }

  function validateApplicationPeriod() {
    if (!applyStartDate?.value || !applyEndDate?.value) {
      setApplicationPeriodMessage(
        "모집 시작 예정일과 모집 마감 예정일을 모두 선택해 주세요.",
        true,
      );
      (applyStartDate?.value ? applyEndDate : applyStartDate)?.focus();
      return false;
    }

    const todayValue = getTodayValue();
    const originalStartDate = applyStartDate.dataset.originalValue;
    const unchangedPastDate = form?.dataset.edit === "true"
      && originalStartDate
      && applyStartDate.value === originalStartDate;

    if (applyStartDate.value < todayValue && !unchangedPastDate) {
      setApplicationPeriodMessage(
        "모집 시작 예정일은 오늘 이후로 선택해 주세요.",
        true,
      );
      applyStartDate.focus();
      return false;
    }

    if (applyEndDate.value < applyStartDate.value) {
      setApplicationPeriodMessage(
        "모집 마감 예정일은 모집 시작 예정일 이후로 선택해 주세요.",
        true,
      );
      applyEndDate.focus();
      return false;
    }

    setApplicationPeriodMessage("", false);
    return true;
  }

  function updateApplicationPeriodLimits() {
    if (!applyStartDate || !applyEndDate) return;

    if (
      form?.dataset.edit !== "true"
      || form?.dataset.scheduledEdit === "true"
      || form?.dataset.draftEdit === "true"
    ) {
      applyStartDate.min = getTodayValue();
    }

    applyEndDate.min = applyStartDate.value || getTodayValue();
    setApplicationPeriodMessage("", false);
  }

  careerType?.addEventListener("change", updateExperienceFields);
  primaryJobCategory?.addEventListener("change", updateJobCategoryOptions);
  salaryText?.addEventListener("change", updateSalaryFields);
  address?.addEventListener("input", updateWorkRegion);
  applyStartDate?.addEventListener("change", updateApplicationPeriodLimits);
  applyEndDate?.addEventListener("change", function () {
    setApplicationPeriodMessage("", false);
  });

  form?.addEventListener("submit", function (event) {
    if (event.submitter?.value === "DRAFT") {
      return;
    }

    if (event.submitter?.value === "REVIEW" && !reviewFormChanged) {
      event.preventDefault();
      showConfirmModal({
        iconClass: "info",
        iconHtml: "!",
        title: "수정 사항이 없습니다",
        message: "숨김 사유를 확인하고 공고 내용을 수정해 주세요.",
        extraHtml: `
          <div class="job-review-modal-notice">
            공고 내용을 한 가지 이상 수정한 후 재검토를 요청할 수 있습니다.
          </div>
        `,
        leftText: "확인",
        rightVisible: false,
      });
      return;
    }

    const experienceValid = validateRange(
      minExperience,
      maxExperience,
      experienceHelp,
      "최소 경력 또는 최대 경력 중 하나를 입력하세요.",
      "최대 경력은 최소 경력 이상이어야 합니다.",
    );
    const salaryValid = validateRange(
      salaryMin,
      salaryMax,
      salaryHelp,
      "최소 연봉 또는 최대 연봉 중 하나를 입력하세요.",
      "최대 연봉은 최소 연봉 이상이어야 합니다.",
    );
    const applicationPeriodValid = validateApplicationPeriod();

    if (!experienceValid || !salaryValid || !applicationPeriodValid) {
      event.preventDefault();
    }
  });

  updateExperienceFields();
  restoreJobCategorySelection();

  if (!jobCategory?.dataset.selectedCategory) {
    updateJobCategoryOptions();
  }
  updateSalaryFields();
  updateWorkRegion();
  updateApplicationPeriodLimits();

  const reviewSubmitButton = document.querySelector("[data-review-submit]");
  if (form?.dataset.hiddenEdit === "true" && reviewSubmitButton) {
    const initialFormState = getComparableFormState(form);

    function getComparableFormState(targetForm) {
      return Array.from(new FormData(targetForm).entries())
        .filter(function ([name]) {
          return name !== "submitType" && !name.startsWith("_");
        })
        .map(function ([name, value]) {
          return [name, String(value).trim()];
        })
        .sort(function (first, second) {
          return (first[0] + first[1]).localeCompare(second[0] + second[1]);
        })
        .map(function ([name, value]) {
          return name + "=" + value;
        })
        .join("&");
    }

    function updateReviewSubmitState() {
      const changed = getComparableFormState(form) !== initialFormState;
      reviewFormChanged = changed;
      reviewSubmitButton.title = changed
        ? "수정한 내용으로 재검토를 요청합니다."
        : "공고 내용을 수정한 후 재검토를 요청할 수 있습니다.";
    }

    form.addEventListener("input", updateReviewSubmitState);
    form.addEventListener("change", updateReviewSubmitState);

    const skillInputs = form.querySelector("[data-skill-hidden-inputs]");
    if (skillInputs) {
      new MutationObserver(updateReviewSubmitState).observe(skillInputs, {
        childList: true,
        subtree: true,
      });
    }

    updateReviewSubmitState();
  }

  const modal = document.querySelector("[data-ai-polish-modal]");
  if (!modal) return;
  const original = modal.querySelector("[data-ai-polish-original]");
  const suggestion = modal.querySelector("[data-ai-polish-suggestion]");
  const summary = modal.querySelector("[data-ai-polish-summary]");
  const error = modal.querySelector("[data-ai-polish-error]");
  const regenerateButton = modal.querySelector("[data-ai-polish-regenerate]");
  const applyButton = modal.querySelector("[data-ai-polish-apply]");
  let target = null;
  let currentFieldType = null;
  let requesting = false;

  const fieldTypes = {
    intro: "INTRODUCTION",
    editIntro: "INTRODUCTION",
    tasks: "MAIN_TASKS",
    editTasks: "MAIN_TASKS",
    requirements: "QUALIFICATIONS",
    editRequirements: "QUALIFICATIONS",
    preferred: "PREFERRED_CONDITIONS",
    editPreferred: "PREFERRED_CONDITIONS",
  };

  function setRequesting(value) {
    requesting = value;
    regenerateButton.disabled = value;
    applyButton.disabled = value;
    regenerateButton.textContent = value ? "생성 중..." : "다시 생성";
  }

  function getInputValue(...selectors) {
    for (const selector of selectors) {
      const element = document.querySelector(selector);
      if (element?.value?.trim()) return element.value.trim();
    }
    return null;
  }

  function getSelectedText(selector) {
    const select = document.querySelector(selector);
    const option = select?.selectedOptions?.[0];
    if (!option?.value) return null;
    return option.textContent.trim();
  }

  function getJobCategoryText() {
    return [
      getSelectedText("#primaryJobCategory"),
      getSelectedText("#category"),
    ]
      .filter(Boolean)
      .join(" - ") || null;
  }

  function getSelectedSkillNames() {
    return Array.from(
      document.querySelectorAll(
        "[data-skill-chip-list] [data-skill-name]",
      ),
    )
      .filter(function (skill) {
        return !skill.hidden;
      })
      .map(function (skill) {
        return (skill.dataset.skillName || skill.textContent).trim();
      })
      .filter(Boolean)
      .filter(function (skill, index, skills) {
        return skills.indexOf(skill) === index;
      })
      .slice(0, 5);
  }

  function getJobContext() {
    return {
      jobTitle: getInputValue("#jobTitle", "#editTitle"),
      jobCategory: getJobCategoryText(),
      employmentType: getSelectedText("#employment"),
      careerType: getSelectedText("#careerType"),
      educationLevel: getSelectedText("#education"),
      workRegion: getInputValue("#workRegion"),
      skillNames: getSelectedSkillNames(),
    };
  }

  async function requestPolishedContent() {
    if (!currentFieldType || !original.value.trim() || requesting) return;

    const previousResult = suggestion.value.trim();
    setRequesting(true);
    error.textContent = "";
    suggestion.value = "AI 수정안을 생성하고 있습니다.";

    try {
      const response = await fetch("/corp/api/job-postings/ai-polish", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fieldType: currentFieldType,
          content: original.value,
          previousResult: previousResult || null,
          ...getJobContext(),
        }),
      });
      const result = await response.json().catch(function () {
        return {};
      });

      if (!response.ok || !result.success) {
        throw new Error(
          result.message || "AI 수정안을 생성하지 못했습니다.",
        );
      }

      suggestion.value = result.polishedContent;
    } catch (requestError) {
      suggestion.value = "";
      error.textContent = requestError.message;
    } finally {
      setRequesting(false);
    }
  }

  function close() {
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }

  function showEmptyPolishWarning(field, itemLabel) {
    const formField = field.closest(".form-field");
    if (!formField) return;

    let warning = formField.querySelector("[data-ai-polish-empty-warning]");
    if (!warning) {
      warning = document.createElement("small");
      warning.className = "ai-polish-empty-warning";
      warning.dataset.aiPolishEmptyWarning = "";
      warning.setAttribute("role", "alert");
      warning.setAttribute("aria-live", "polite");
      field.insertAdjacentElement("afterend", warning);
    }

    warning.textContent = `${itemLabel} 내용을 먼저 입력해 주세요.`;
    field.classList.add("is-ai-polish-empty");
    field.focus();

    field.addEventListener(
      "input",
      function clearEmptyPolishWarning() {
        field.classList.remove("is-ai-polish-empty");
        warning.remove();
      },
      { once: true },
    );
  }

  document.querySelectorAll("[data-ai-polish-open]").forEach(function (button) {
    button.addEventListener("click", async function () {
      target = document.querySelector(button.dataset.aiPolishOpen);
      const itemLabel = button.dataset.aiPolishLabel || "상세 내용";
      if (!target || !target.value.trim()) {
        if (target) showEmptyPolishWarning(target, itemLabel);
        return;
      }
      currentFieldType = fieldTypes[target.id];
      summary.textContent = itemLabel;
      original.value = target.value;
      suggestion.value = "";
      error.textContent = "";
      modal.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";
      await requestPolishedContent();
    });
  });
  modal.querySelectorAll("[data-ai-polish-close]").forEach(function (button) {
    button.addEventListener("click", close);
  });
  modal
    .querySelector("[data-ai-polish-regenerate]")
    .addEventListener("click", async function () {
      await requestPolishedContent();
    });
  modal
    .querySelector("[data-ai-polish-apply]")
    .addEventListener("click", function () {
      if (!suggestion.value.trim()) {
        error.textContent = "적용할 수정안이 없습니다.";
        return;
      }
      if (target) {
        target.value = suggestion.value;
        target.dispatchEvent(new Event("input", { bubbles: true }));
      }
      close();
    });
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && modal.getAttribute("aria-hidden") === "false")
      close();
  });
});
