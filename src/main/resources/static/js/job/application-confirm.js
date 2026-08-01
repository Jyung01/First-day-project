document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("[data-application-form]");

  form?.addEventListener("submit", (event) => {
    event.preventDefault();

    showConfirmModal({
      iconClass: "info",
      iconHtml: "?",
      title: "최종 지원하시겠어요?",
      message:
        "제출 후에는 이력서와 자기소개서를 수정할 수 없습니다.\n선택한 내용으로 지원하시겠어요?",
      leftText: "다시 확인",
      rightText: "지원하기",
      onRight: () => form.submit(),
    });
  });
});
