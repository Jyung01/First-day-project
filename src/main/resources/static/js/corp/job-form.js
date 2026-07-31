document.addEventListener("DOMContentLoaded", function () {
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
