document.addEventListener("DOMContentLoaded", () => {
  const applyAdminModal = (modalId, danger = false) => {
    const modal = document.getElementById(modalId);
    const rightButton = modal?.querySelector(".modal-footer .btn:last-child");

    modal?.classList.add("admin-common-modal");

    if (danger && rightButton) {
      rightButton.className = "btn btn-danger";
    }
  };

  const hideJobBody = () => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="hideReason">숨김 사유 *</label>
        <textarea
          id="hideReason"
          maxlength="1000"
          placeholder="위반 또는 부적절한 채용 정보를 입력하세요."
        ></textarea>
        <p class="admin-reason-error" aria-live="polite">
          숨김 사유를 입력해주세요.
        </p>
      </div>
      <p class="modal-notice modal-notice--danger">
        숨김 처리 후 기업회원은 수정 뒤 재검토를 요청할 수 있습니다.
        입력한 숨김 사유는 기업회원에게 표시됩니다.
      </p>
    </div>
  `;

  const keepHiddenBody = () => `
    <div class="modal-form-body">
      <div class="form-group admin-maintain-reason">
        <label for="reviewHideReason">숨김 사유 *</label>
        <textarea
          id="reviewHideReason"
          placeholder="숨김 사유를 입력하세요."
        ></textarea>
        <p class="admin-reason-error" aria-live="polite">
          숨김 사유를 입력해주세요.
        </p>
      </div>
      <p class="modal-notice">
        기존 숨김 사유를 그대로 사용하거나 필요한 경우 수정할 수 있습니다.
        최종 숨김 사유는 기업회원에게 표시됩니다.
      </p>
    </div>
  `;

  const openHideModal = (button) => {
    showFormModal({
      title: "채용공고 숨김 처리",
      bodyHtml: hideJobBody(),
      leftText: "취소",
      rightText: "숨김 처리",
      onRight: () => {
        const reasonInput = document.querySelector("#hideReason");
        const error = reasonInput
          ?.closest(".form-group")
          ?.querySelector(".admin-reason-error");
        const form = document.getElementById(button.dataset.formId);
        const reasonField = form?.querySelector("[data-job-hide-reason]");
        const reason = reasonInput?.value.trim() || "";

        if (!reason) {
          error?.classList.add("is-visible");
          reasonInput?.focus();
          return;
        }

        if (!form || !reasonField) return;

        reasonField.value = reason;
        closeModal("formModal");
        form.requestSubmit();
      },
    });
    applyAdminModal("formModal", true);

    const reasonInput = document.querySelector("#hideReason");
    const error = reasonInput
      ?.closest(".form-group")
      ?.querySelector(".admin-reason-error");

    reasonInput?.addEventListener("input", () => {
      error?.classList.remove("is-visible");
    });
  };

  const openKeepHiddenModal = (button) => {
    showFormModal({
      title: "채용공고 숨김 유지",
      bodyHtml: keepHiddenBody(),
      leftText: "취소",
      rightText: "숨김 유지",
      onRight: () => {
        const reasonInput = document.querySelector("#reviewHideReason");
        const error = document.querySelector(".admin-reason-error");
        const form = document.getElementById(button.dataset.formId);
        const reasonField = form?.querySelector("[data-job-keep-reason]");
        const reason = reasonInput?.value.trim() || "";

        if (!reason) {
          error?.classList.add("is-visible");
          reasonInput?.focus();
          return;
        }

        if (!form || !reasonField) return;

        reasonField.value = reason;
        closeModal("formModal");
        form.requestSubmit();
      },
    });
    applyAdminModal("formModal", true);

    const reasonInput = document.querySelector("#reviewHideReason");
    const error = document.querySelector(".admin-reason-error");

    if (reasonInput) {
      reasonInput.value = button.dataset.hiddenReason || "";
    }

    reasonInput?.addEventListener("input", () => {
      error?.classList.remove("is-visible");
    });
  };

  const openReleaseHiddenModal = (button) => {
    showConfirmModal({
      iconClass: "info",
      iconHtml: "✓",
      title: "채용공고 숨김 해제",
      message: "해당 채용공고의 숨김을 해제할까요?",
      extraHtml: `
        <p class="modal-notice">
          마감일이 남은 공고는 다시 노출되며, 마감일이 지난 공고는 마감
          상태로 변경됩니다.
        </p>
      `,
      leftText: "취소",
      rightText: "숨김 해제",
      rightClass: "btn-primary",
      onRight: () => {
        const form = document.getElementById(button.dataset.formId);
        form?.requestSubmit();
      },
    });
    applyAdminModal("confirmModal");
  };

  document.querySelectorAll("[data-modal-open]").forEach((button) => {
    button.addEventListener("click", () => {
      const modalType = button.dataset.modalOpen;

      if (modalType === "job-hide") {
        openHideModal(button);
      }

      if (modalType === "job-keep-hidden") {
        openKeepHiddenModal(button);
      }

      if (modalType === "job-release-hidden") {
        openReleaseHiddenModal(button);
      }
    });
  });
});
