document.addEventListener("DOMContentLoaded", () => {
  const modal = document.querySelector("[data-resume-modal]");
  const openButton = document.querySelector("[data-resume-modal-open]");
  const closeButtons = modal?.querySelectorAll("[data-resume-modal-close]");
  const cancelButton = modal?.querySelector("[data-resume-modal-cancel]");

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
    openButton?.focus();
  };

  openButton?.addEventListener("click", openModal);
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

  modal?.querySelectorAll('input[name="resumeId"]').forEach((radio) => {
    radio.addEventListener("change", () => {
      modal.querySelectorAll(".selected-label").forEach((label) => label.textContent = "선택");
      radio.closest(".resume-modal-option")
        ?.querySelector(".selected-label")
        .replaceChildren("선택됨");
    });
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && modal?.classList.contains("is-open")) closeModal();
  });

  if (new URLSearchParams(window.location.search).get("apply") === "true") {
    openModal();
  }
});
