document.addEventListener("DOMContentLoaded", () => {
  const modal = document.getElementById("formModal");

  if (!modal) return;

  const escapeHtml = (value = "") => String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

  const applyVersionModal = () => {
    modal.classList.add("admin-common-modal", "version-modal");
  };

  const addCloseButton = () => {
    const dialog = modal.querySelector(".modal");

    if (!dialog || dialog.querySelector(".version-modal-close")) return;

    const closeButton = document.createElement("button");
    closeButton.type = "button";
    closeButton.className = "version-modal-close";
    closeButton.setAttribute("aria-label", "모달 닫기");
    closeButton.textContent = "×";
    closeButton.addEventListener("click", () => closeModal("formModal"));
    dialog.appendChild(closeButton);
  };

  const viewBody = (versionName, changeNotes) => `
    <div class="modal-form-body">
      <div class="version-modal-table">
        <div class="version-modal-label">버전</div>
        <div class="version-modal-value">${escapeHtml(versionName)}</div>
        <div class="version-modal-label">변경내역</div>
        <div class="version-modal-value version-modal-value--notes">${escapeHtml(changeNotes)}</div>
      </div>
    </div>
  `;

  const formBody = (versionName = "", changeNotes = "") => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="siteVersionName">버전</label>
        <input
          id="siteVersionName"
          type="text"
          maxlength="50"
          value="${escapeHtml(versionName)}"
          placeholder="예: 0.0.5-SNAPSHOT"
        />
      </div>
      <div class="form-group">
        <label for="siteVersionNotes">변경내역</label>
        <textarea
          id="siteVersionNotes"
          placeholder="이번 버전에서 변경된 주요 기능과 화면을 입력하세요."
        >${escapeHtml(changeNotes)}</textarea>
      </div>
      <p class="version-field-error" role="alert"></p>
    </div>
  `;

  const submitVersion = (form, siteVersionId = null) => {
    const versionName = document.getElementById("siteVersionName")?.value.trim();
    const changeNotes = document.getElementById("siteVersionNotes")?.value.trim();
    const error = modal.querySelector(".version-field-error");

    if (!versionName || !changeNotes) {
      if (error) {
        error.textContent = !versionName
          ? "버전을 입력해 주세요."
          : "변경내역을 입력해 주세요.";
      }
      document.getElementById(!versionName ? "siteVersionName" : "siteVersionNotes")?.focus();
      return;
    }

    if (siteVersionId) {
      form.action = `/admin/config/version/${siteVersionId}`;
    }
    form.querySelector("[data-version-name-field]").value = versionName;
    form.querySelector("[data-version-notes-field]").value = changeNotes;
    form.submit();
  };

  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-version-action]");

    if (!button) return;

    const action = button.dataset.versionAction;
    const siteVersionId = button.dataset.versionId;
    const versionName = button.dataset.versionName || "";
    const changeNotes = button.dataset.versionNotes || "";

    if (action === "view") {
      showFormModal({
        title: "버전 변경내역",
        bodyHtml: viewBody(versionName, changeNotes),
        leftText: "닫기",
        rightText: "",
        onLeft: null,
      });
      modal.querySelector(".modal-footer").classList.add("one-button");
    }

    if (action === "create") {
      showFormModal({
        title: "버전 등록",
        bodyHtml: formBody(),
        leftText: "취소",
        rightText: "등록",
        onRight: () => submitVersion(
          document.getElementById("versionCreateForm"),
        ),
      });
      modal.querySelector(".modal-footer").classList.remove("one-button");
    }

    if (action === "edit") {
      showFormModal({
        title: "버전 수정",
        bodyHtml: formBody(versionName, changeNotes),
        leftText: "취소",
        rightText: "저장",
        onRight: () => submitVersion(
          document.getElementById("versionEditForm"),
          siteVersionId,
        ),
      });
      modal.querySelector(".modal-footer").classList.remove("one-button");
    }

    applyVersionModal();
    addCloseButton();
  });
});
