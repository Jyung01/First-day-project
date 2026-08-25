document.addEventListener("DOMContentLoaded", () => {
  const applyAdminModal = (modalId) => {
    document.getElementById(modalId)?.classList.add("admin-common-modal");
  };

  const createCategoryOptions = (selectedCategory) =>
    Array.from(
      document.querySelectorAll("[data-root-category-source] option"),
    )
      .map((option) => {
        const isSelected = option.value === String(selectedCategory)
          || option.textContent === selectedCategory;
        const selected = isSelected ? " selected" : "";

        return `
          <option value="${option.value}"${selected}>
            ${option.textContent}
          </option>
        `;
      })
      .join("");

  const categoryCreateBody = () => `
    <div class="modal-form-body">
      <div class="modal-form-grid">
        <div class="form-group">
          <label for="categoryType">구분</label>
          <select id="categoryType">
            <option value="SECONDARY">2차 카테고리</option>
            <option value="PRIMARY">1차 카테고리</option>
          </select>
        </div>
        <div class="form-group" data-parent-category-field>
          <label for="parentCategory">상위 카테고리</label>
          <select id="parentCategory">
            ${createCategoryOptions("")}
            <option
              value=""
              data-primary-category-option
              hidden
            >
              해당 없음 (1차 카테고리)
            </option>
          </select>
        </div>
        <div class="form-group form-group--wide">
          <label for="categoryName">카테고리명</label>
          <input
            id="categoryName"
            placeholder="카테고리명을 입력하세요."
          />
        </div>
      </div>
      <p class="modal-notice">
        등록한 직무는 이력서·채용공고의 직무 선택 목록에 바로 노출됩니다.
        사용 중인 직무는 이름만 수정할 수 있습니다.
      </p>
    </div>
  `;

  const primaryCategoryEditBody = () => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="categoryName">1차 카테고리명</label>
        <input
          id="categoryName"
          placeholder="카테고리명을 입력하세요."
        />
      </div>
      <p class="modal-notice">
        카테고리명만 변경할 수 있으며, 소속된 하위 직무는 그대로 유지됩니다.
      </p>
    </div>
  `;

  const secondaryCategoryEditBody = (parentCategory, usageCount) => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="parentCategory">1차 카테고리</label>
        <select id="parentCategory"${usageCount > 0 ? " disabled" : ""}>
          ${createCategoryOptions(parentCategory)}
        </select>
      </div>
      <div class="form-group">
        <label for="categoryName">2차 카테고리명</label>
        <input
          id="categoryName"
          placeholder="카테고리명을 입력하세요."
        />
      </div>
      <p class="modal-notice">
        ${usageCount > 0
          ? "사용 중인 직무는 이름만 수정할 수 있습니다."
          : "2차 카테고리의 소속과 이름을 변경할 수 있습니다."}
      </p>
    </div>
  `;

  const createSkillCategoryOptions = (selectedCategory) =>
    Array.from(
      document.querySelectorAll("[data-skill-group-source] option"),
    )
      .map((option) => {
        const isSelected = option.value === String(selectedCategory)
          || option.textContent === selectedCategory;
        const selected = isSelected ? " selected" : "";

        return `
          <option value="${option.value}"${selected}>
            ${option.textContent}
          </option>
        `;
      })
      .join("");

  const skillCreateBody = () => `
    <div class="modal-form-body">
      <div class="modal-form-grid">
        <div class="form-group">
          <label for="skillType">구분</label>
          <select id="skillType">
            <option value="SKILL">기술</option>
            <option value="GROUP">기술 분류</option>
          </select>
        </div>
        <div
          class="form-group"
          data-skill-category-field
        >
          <label for="skillCategory">기술 분류</label>
          <select id="skillCategory">
            ${createSkillCategoryOptions("")}
            <option
              value=""
              data-skill-group-option
              hidden
            >
              해당 없음 (기술 분류)
            </option>
          </select>
        </div>
        <div class="form-group form-group--wide">
          <label for="skillName" data-skill-name-label>기술명</label>
          <input
            id="skillName"
            placeholder="기술명을 입력하세요."
          />
        </div>
      </div>
      <p class="modal-notice" data-skill-create-notice>
        기술은 선택한 기술 분류에 등록됩니다.
      </p>
    </div>
  `;

  const skillBody = (selectedCategory, usageCount) => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="skillCategory">기술 분류</label>
        <select id="skillCategory"${usageCount > 0 ? " disabled" : ""}>
          ${createSkillCategoryOptions(selectedCategory)}
        </select>
      </div>
      <div class="form-group">
        <label for="skillName">기술명</label>
        <input
          id="skillName"
          placeholder="기술명을 입력하세요."
        />
      </div>
      <p class="modal-notice">
        ${usageCount > 0
          ? "사용 중인 기술은 이름만 수정할 수 있습니다."
          : "기술 분류와 기술명을 변경할 수 있습니다."}
      </p>
    </div>
  `;

  const skillGroupBody = () => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="skillGroupName">기술 분류명</label>
        <input
          id="skillGroupName"
          placeholder="기술 분류명을 입력하세요."
        />
      </div>
      <p class="modal-notice">
        등록한 기술 분류는 기술 등록·수정 화면의 선택 항목에 표시됩니다.
      </p>
    </div>
  `;

  const submitJobCategoryCreate = () => {
    const form = document.querySelector("#jobCategoryCreateForm");
    const categoryType = document.querySelector("#categoryType")?.value;
    const categoryName = document.querySelector("#categoryName")?.value.trim();
    const parentId = document.querySelector("#parentCategory")?.value || "";

    if (!form || !categoryName) {
      document.querySelector("#categoryName")?.focus();
      return;
    }

    if (categoryType === "SECONDARY" && !parentId) {
      document.querySelector("#parentCategory")?.focus();
      return;
    }

    form.querySelector("[data-category-form-type]").value = categoryType;
    form.querySelector("[data-category-form-name]").value = categoryName;
    form.querySelector("[data-category-form-parent]").value =
      categoryType === "PRIMARY" ? "" : parentId;
    form.submit();
  };

  const submitJobCategoryEdit = (categoryId) => {
    const form = document.querySelector("#jobCategoryEditForm");
    const categoryName = document.querySelector("#categoryName")?.value.trim();
    const parentId = document.querySelector("#parentCategory")?.value || "";

    if (!form || !categoryId || !categoryName) {
      document.querySelector("#categoryName")?.focus();
      return;
    }

    form.querySelector("[data-category-edit-id]").value = categoryId;
    form.querySelector("[data-category-edit-name]").value = categoryName;
    form.querySelector("[data-category-edit-parent]").value = parentId;
    form.submit();
  };

  const submitJobCategoryDelete = (categoryId) => {
    const form = document.querySelector("#jobCategoryDeleteForm");

    if (!form || !categoryId) return;

    form.querySelector("[data-category-delete-id]").value = categoryId;
    form.submit();
  };

  const submitJobCategoryRestore = (categoryId) => {
    const form = document.querySelector("#jobCategoryRestoreForm");

    if (!form || !categoryId) return;

    form.querySelector("[data-category-restore-id]").value = categoryId;
    form.submit();
  };

  const showCategoryRestorePrompt = () => {
    const prompt = document.querySelector(
      "[data-category-restore-prompt]",
    );

    if (!prompt) return;

    const categoryId = prompt.dataset.categoryId;
    const categoryName = prompt.dataset.categoryName;
    const isPrimary = prompt.dataset.categoryDepth === "1";
    const restoreNotice = isPrimary
      ? "복구하면 기존 하위 직무와 사용 이력이 유지됩니다. 취소하면 등록되지 않습니다."
      : "복구하면 기존 사용 이력이 유지됩니다. 취소하면 등록되지 않습니다.";

    showConfirmModal({
      iconClass: "info",
      iconHtml: "↻",
      title: "삭제된 카테고리 복구",
      message: `'${categoryName}' 카테고리를 복구할까요?`,
      extraHtml: `
        <p class="modal-notice">
          ${restoreNotice}
        </p>
      `,
      leftText: "취소",
      rightText: "복구",
      onRight: () => submitJobCategoryRestore(categoryId),
    });
    applyAdminModal("confirmModal");
  };

  const submitSkillCreate = () => {
    const form = document.querySelector("#skillCreateForm");
    const skillType = document.querySelector("#skillType")?.value;
    const skillName = document.querySelector("#skillName")?.value.trim();
    const parentId = document.querySelector("#skillCategory")?.value || "";

    if (!form || !skillName) {
      document.querySelector("#skillName")?.focus();
      return;
    }

    if (skillType === "SKILL" && !parentId) {
      document.querySelector("#skillCategory")?.focus();
      return;
    }

    form.querySelector("[data-skill-form-type]").value = skillType;
    form.querySelector("[data-skill-form-name]").value = skillName;
    form.querySelector("[data-skill-form-parent]").value =
      skillType === "GROUP" ? "" : parentId;
    form.submit();
  };

  const submitSkillEdit = (skillId, isGroup) => {
    const form = document.querySelector("#skillEditForm");
    const nameSelector = isGroup ? "#skillGroupName" : "#skillName";
    const skillName = document.querySelector(nameSelector)?.value.trim();
    const parentId = isGroup
      ? ""
      : document.querySelector("#skillCategory")?.value || "";

    if (!form || !skillId || !skillName) {
      document.querySelector(nameSelector)?.focus();
      return;
    }

    form.querySelector("[data-skill-edit-id]").value = skillId;
    form.querySelector("[data-skill-edit-name]").value = skillName;
    form.querySelector("[data-skill-edit-parent]").value = parentId;
    form.submit();
  };

  const submitSkillDelete = (skillId) => {
    const form = document.querySelector("#skillDeleteForm");

    if (!form || !skillId) return;

    form.querySelector("[data-skill-delete-id]").value = skillId;
    form.submit();
  };

  const submitSkillRestore = (skillId) => {
    const form = document.querySelector("#skillRestoreForm");

    if (!form || !skillId) return;

    form.querySelector("[data-skill-restore-id]").value = skillId;
    form.submit();
  };

  const showSkillRestorePrompt = () => {
    const prompt = document.querySelector("[data-skill-restore-prompt]");

    if (!prompt) return;

    const skillId = prompt.dataset.skillId;
    const skillName = prompt.dataset.skillName;
    const isGroup = prompt.dataset.skillDepth === "1";
    const restoreNotice = isGroup
      ? "복구하면 기존 소속 기술과 사용 이력이 유지됩니다. 취소하면 등록되지 않습니다."
      : "복구하면 기존 사용 이력이 유지됩니다. 취소하면 등록되지 않습니다.";

    showConfirmModal({
      iconClass: "info",
      iconHtml: "↻",
      title: "삭제된 기술 항목 복구",
      message: `'${skillName}' 항목을 복구할까요?`,
      extraHtml: `
        <p class="modal-notice">
          ${restoreNotice}
        </p>
      `,
      leftText: "취소",
      rightText: "복구",
      onRight: () => submitSkillRestore(skillId),
    });
    applyAdminModal("confirmModal");
  };

  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-modal-open]");

    if (!button) return;

    const modalType = button.dataset.modalOpen;
    const modalId = button.dataset.modalId || "";
    const title = button.dataset.modalTitle;
    const value = button.dataset.modalValue || "";
    const categoryLevel = button.dataset.categoryLevel;
    const parentCategory = button.dataset.parentCategory || "";
    const parentCategoryId = button.dataset.parentId || parentCategory;
    const skillCategory = button.dataset.skillCategory || "언어";
    const skillCategoryId = button.dataset.parentId || skillCategory;
    const skillAction = button.dataset.skillAction || "edit";
    const usageCount = Number(button.dataset.usageCount || 0);
    const isPrimaryCategory = categoryLevel === "primary";

    if (modalType === "category") {
        let bodyHtml = categoryCreateBody();

        if (categoryLevel === "primary") {
          bodyHtml = primaryCategoryEditBody();
        }

        if (categoryLevel === "secondary") {
          bodyHtml = secondaryCategoryEditBody(
            parentCategoryId,
            usageCount,
          );
        }

        showFormModal({
          title: title || "카테고리 등록",
          bodyHtml,
          onRight: categoryLevel
            ? () => submitJobCategoryEdit(modalId)
            : submitJobCategoryCreate,
        });
        applyAdminModal("formModal");
        document.querySelector("#categoryName").value = value;
    }

    if (modalType === "skill") {
        const bodyHtml = skillAction === "create"
          ? skillCreateBody()
          : skillBody(skillCategoryId, usageCount);

        showFormModal({
          title: title || "기술 등록",
          bodyHtml,
          onRight: skillAction === "create"
            ? submitSkillCreate
            : () => submitSkillEdit(modalId, false),
        });
        applyAdminModal("formModal");
        document.querySelector("#skillName").value = value;
    }

    if (modalType === "skill-group") {
        showFormModal({
          title: title || "기술 분류 등록",
          bodyHtml: skillGroupBody(),
          onRight: () => submitSkillEdit(modalId, true),
        });
        applyAdminModal("formModal");
        document.querySelector("#skillGroupName").value = value;
    }

    if (modalType === "category-delete") {
        const deleteNotice = isPrimaryCategory
          ? "하위 직무도 함께 목록에서 제외됩니다. 기존 채용공고의 직무 정보는 유지됩니다."
          : "기존 채용공고의 직무 정보는 유지되며, 새 등록 화면에서는 표시되지 않습니다.";

        showConfirmModal({
          iconClass: "danger",
          iconHtml: "!",
          title: "카테고리 삭제",
          message: `'${value}' 카테고리를 삭제할까요?`,
          extraHtml: `
            <p class="modal-notice modal-notice--danger">
              ${deleteNotice}
            </p>
          `,
          rightText: "삭제",
          rightClass: "btn-danger",
          onRight: () => submitJobCategoryDelete(modalId),
        });
        applyAdminModal("confirmModal");
    }

    if (modalType === "skill-delete") {
        showConfirmModal({
          iconClass: "danger",
          iconHtml: "!",
          title: "기술 스택 삭제",
          message: `'${value}' 기술 스택을 삭제할까요?`,
          extraHtml: `
            <p class="modal-notice modal-notice--danger">
              삭제해도 기존 이력서·채용공고의 기술 정보는 유지됩니다.
            </p>
          `,
          rightText: "삭제",
          rightClass: "btn-danger",
          onRight: () => submitSkillDelete(modalId),
        });
        applyAdminModal("confirmModal");
    }

    if (modalType === "skill-group-delete") {
        showConfirmModal({
          iconClass: "danger",
          iconHtml: "!",
          title: "기술 분류 삭제",
          message: `'${value}' 기술 분류를 삭제할까요?`,
          extraHtml: `
            <p class="modal-notice modal-notice--danger">
              소속 기술도 새 등록 화면에서 함께 제외됩니다.
              기존 이력서와 채용공고의 기술 정보는 유지됩니다.
            </p>
          `,
          rightText: "삭제",
          rightClass: "btn-danger",
          onRight: () => submitSkillDelete(modalId),
        });
        applyAdminModal("confirmModal");
      }
  });

  document.addEventListener("change", (event) => {
    if (event.target.id === "categoryType") {
      const parentField = document.querySelector(
        "[data-parent-category-field]",
      );
      const parentSelect = parentField?.querySelector("#parentCategory");
      const primaryOption = parentSelect?.querySelector(
        "[data-primary-category-option]",
      );
      const isPrimary = event.target.value === "PRIMARY";

      if (!parentSelect || !primaryOption) return;

      primaryOption.hidden = !isPrimary;
      parentSelect.disabled = isPrimary;

      if (isPrimary) {
        parentSelect.value = "";
      } else {
        parentSelect.value = parentSelect.querySelector(
          "option:not([data-primary-category-option])",
        )?.value || "";
      }

      return;
    }

    if (event.target.id !== "skillType") return;

    const isGroup = event.target.value === "GROUP";
    const categoryField = document.querySelector(
      "[data-skill-category-field]",
    );
    const categorySelect = categoryField?.querySelector("#skillCategory");
    const groupOption = categorySelect?.querySelector(
      "[data-skill-group-option]",
    );
    const nameLabel = document.querySelector("[data-skill-name-label]");
    const nameInput = document.querySelector("#skillName");
    const notice = document.querySelector("[data-skill-create-notice]");

    if (categorySelect && groupOption) {
      groupOption.hidden = !isGroup;
      categorySelect.disabled = isGroup;

      if (isGroup) {
        categorySelect.value = "";
      } else {
        categorySelect.value = categorySelect.querySelector(
          "option:not([data-skill-group-option])",
        )?.value || "";
      }
    }

    if (nameLabel) {
      nameLabel.textContent = isGroup ? "기술 분류명" : "기술명";
    }

    if (nameInput) {
      nameInput.placeholder = isGroup
        ? "기술 분류명을 입력하세요."
        : "기술명을 입력하세요.";
    }

    if (notice) {
      notice.textContent = isGroup
        ? "등록한 기술 분류는 기술 등록·수정 화면에 표시됩니다."
        : "기술은 선택한 기술 분류에 등록됩니다.";
    }
  });

  showCategoryRestorePrompt();
  showSkillRestorePrompt();
});
