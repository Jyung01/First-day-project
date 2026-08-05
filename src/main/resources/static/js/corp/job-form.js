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

  careerType?.addEventListener("change", updateExperienceFields);
  primaryJobCategory?.addEventListener("change", updateJobCategoryOptions);
  salaryText?.addEventListener("change", updateSalaryFields);
  address?.addEventListener("input", updateWorkRegion);

  form?.addEventListener("submit", function (event) {
    if (event.submitter?.value === "DRAFT") {
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

    if (!experienceValid || !salaryValid) {
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

  const modal = document.querySelector("[data-ai-polish-modal]");
  if (!modal) return;
  const original = modal.querySelector("[data-ai-polish-original]");
  const suggestion = modal.querySelector("[data-ai-polish-suggestion]");
  const label = modal.querySelector("[data-ai-polish-label]");
  let target = null;

  function makeSuggestion(value) {
    const text = value.trim();
    if (!text) return "다듬을 문장을 먼저 입력해 주세요.";
    return text.replace(/합니다\.?/g, "합니다.").replace(/\n{3,}/g, "\n\n");
  }
  function close() {
    modal.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
  }
  document.querySelectorAll("[data-ai-polish-open]").forEach(function (button) {
    button.addEventListener("click", function () {
      target = document.querySelector(button.dataset.aiPolishOpen);
      if (!target || !target.value.trim()) {
        target?.focus();
        return;
      }
      label.textContent = button.dataset.aiPolishLabel || "상세 내용";
      original.value = target.value;
      suggestion.value = makeSuggestion(target.value);
      modal.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";
    });
  });
  modal.querySelectorAll("[data-ai-polish-close]").forEach(function (button) {
    button.addEventListener("click", close);
  });
  modal
    .querySelector("[data-ai-polish-regenerate]")
    .addEventListener("click", function () {
      suggestion.value = makeSuggestion(original.value);
    });
  modal
    .querySelector("[data-ai-polish-apply]")
    .addEventListener("click", function () {
      if (target) target.value = suggestion.value;
      close();
    });
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && modal.getAttribute("aria-hidden") === "false")
      close();
  });
});
