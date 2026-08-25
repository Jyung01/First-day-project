document.addEventListener("DOMContentLoaded", () => {
  const modal = document.querySelector("[data-resume-modal]");
  const openButtons = document.querySelectorAll("[data-resume-modal-open]");
  const defaultOpenButton = openButtons[0];
  let lastOpenButton = defaultOpenButton;
  const closeButtons = modal?.querySelectorAll("[data-resume-modal-close]");
  const cancelButton = modal?.querySelector("[data-resume-modal-cancel]");
  const applicationError = modal?.dataset.applicationError;

  const openModal = () => {
    if (!modal) return;
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
    document.body.classList.add("resume-modal-open");
    modal.querySelector('input[name="resumeId"]:checked')?.focus();
  };

  const closeModal = () => {
    if (!modal) return;
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("resume-modal-open");
    const url = new URL(window.location.href);
    if (url.searchParams.has("apply")) {
      url.searchParams.delete("apply");
      window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
    }
    lastOpenButton?.focus();
  };

  const showClosedPostingModal = (openButton = defaultOpenButton) => {
    const unavailableMessage = openButton
      ?.dataset.applicationUnavailableMessage
      || "현재 지원할 수 없는 채용공고입니다.\n"
        + "공고 상태를 확인한 후 다시 시도해주세요.";

    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "지원할 수 없습니다",
      message: unavailableMessage,
      leftVisible: false,
      rightText: "확인",
    });
  };

  openButtons.forEach((openButton) => {
    openButton.addEventListener("click", () => {
      lastOpenButton = openButton;

      if (openButton.dataset.acceptingApplications !== "true") {
        showClosedPostingModal(openButton);
        return;
      }

      if (openButton.dataset.alreadyApplied === "true") {
        showAlreadyAppliedModal();
        return;
      }

      if (openButton.dataset.personalMember === "true") {
        openModal();
        return;
      }

      showConfirmModal({
        iconClass: "info",
        iconHtml: "?",
        title: "로그인이 필요한 기능입니다",
        message:
          "입사지원은 개인회원 로그인 후 이용할 수 있습니다.\n" +
          "로그인 후 다시 이용해주세요.",
        leftText: "취소",
        rightText: "로그인",
        onRight: () => {
          const returnUrl = new URL(window.location.href);
          returnUrl.searchParams.set("apply", "true");
          const loginUrl = document.body.dataset.loginUrl || "/auth/login";
          window.location.href =
            `${loginUrl}?returnUrl=${encodeURIComponent(
              returnUrl.pathname + returnUrl.search,
            )}`;
        },
      });
    });
  });
  closeButtons?.forEach((button) => button.addEventListener("click", closeModal));
  cancelButton?.addEventListener("click", () => {
    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "지원을 취소할까요?",
      message:
        "선택한 이력서와 자기소개서 정보가 초기화됩니다.\n채용공고 상세로 돌아갈까요?",
      leftText: "계속 작성",
      rightText: "지원 취소",
      rightClass: "btn-danger",
      onRight: closeModal,
    });
  });

  modal?.addEventListener("click", (event) => {
    if (event.target === modal) closeModal();
  });

  ["resumeId", "coverLetterId"].forEach((fieldName) => {
    modal?.querySelectorAll(`input[name="${fieldName}"]`).forEach((radio) => {
      radio.addEventListener("change", () => {
        const section = radio.closest(".resume-modal-section");

        section?.querySelectorAll(".selected-label")
          .forEach((label) => label.textContent = "선택");
        radio.closest(".resume-modal-option")
          ?.querySelector(".selected-label")
          .replaceChildren("선택됨");
      });
    });
  });

  const showAlreadyAppliedModal = () => {
    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "이미 지원한 공고입니다",
      message:
        "동일한 채용공고에는 한 번만 지원할 수 있습니다.\n" +
        "마이페이지에서 기존 지원 내역을 확인해주세요.",
      leftText: "닫기",
      rightText: "지원 내역 확인",
      rightClass: "btn-danger",
      onRight: () => {
        window.location.href = "/my/applications";
      },
    });
  };

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && modal?.classList.contains("is-open")) closeModal();
  });

  if (applicationError) {
    showConfirmModal({
      iconClass: "danger",
      iconHtml: "!",
      title: "지원할 수 없습니다",
      message: applicationError,
      leftVisible: false,
      rightText: "확인",
    });
  } else if (
    defaultOpenButton?.dataset.personalMember === "true"
    && new URLSearchParams(window.location.search).get("apply") === "true"
  ) {
    if (defaultOpenButton.dataset.acceptingApplications !== "true") {
      showClosedPostingModal(defaultOpenButton);
    } else if (defaultOpenButton.dataset.alreadyApplied === "true") {
      showAlreadyAppliedModal();
    } else {
      openModal();
    }
  }
});
