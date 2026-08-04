document.addEventListener("DOMContentLoaded", () => {
  const categoryList = document.querySelector("[data-category-list]");
  const detail = document.querySelector("[data-category-detail]");
  const title = document.querySelector("[data-category-title]");

  if (!categoryList || !detail || !title) return;

  const categoryData = {
    "개발·데이터": [
      ["백엔드 개발", 224],
      ["프론트엔드 개발", 198],
      ["데이터 엔지니어", 146],
      ["AI·머신러닝", 118],
      ["DevOps·인프라", 92],
      ["모바일 앱 개발", 84],
      ["QA·테스트", 61],
      ["보안", 47],
    ],
    "기획·PM": [
      ["서비스 기획", 112],
      ["프로덕트 매니저", 96],
      ["사업 기획", 73],
      ["전략 기획", 52],
      ["프로젝트 매니저", 48],
    ],
    디자인: [
      ["UI·UX 디자인", 104],
      ["웹 디자인", 71],
      ["그래픽 디자인", 58],
      ["브랜드 디자인", 43],
    ],
    "마케팅·광고": [
      ["콘텐츠 마케팅", 87],
      ["퍼포먼스 마케팅", 76],
      ["브랜드 마케팅", 59],
      ["광고 기획", 41],
    ],
    영업: [
      ["B2B 영업", 93],
      ["국내 영업", 81],
      ["해외 영업", 44],
      ["영업 관리", 38],
    ],
    "경영·지원": [
      ["인사", 89],
      ["총무", 57],
      ["재무·회계", 78],
      ["법무", 31],
      ["경영지원", 66],
    ],
    금융: [
      ["금융 분석", 42],
      ["투자·자산운용", 35],
      ["리스크 관리", 27],
    ],
  };

  const escapeHtml = (value) =>
    value.replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");

  const createRow = ([name, count], parentCategory) => {
    const safeName = escapeHtml(name);
    const safeParentCategory = escapeHtml(parentCategory);

    return `
      <div class="config-row">
        <strong>${safeName}</strong>
        <span>${count}건</span>
        <div class="config-actions">
          <button
            class="table-action"
            data-modal-open="category"
            data-modal-title="카테고리 수정"
            data-modal-value="${safeName}"
            data-category-level="secondary"
            data-parent-category="${safeParentCategory}"
          >수정</button>
          <button
            class="table-action"
            data-modal-open="category-delete"
            data-modal-value="${safeName}"
            data-category-level="secondary"
          >삭제</button>
        </div>
      </div>
    `;
  };

  const renderCategory = (category) => {
    categoryList.querySelectorAll("[data-category]").forEach((button) => {
      button.closest(".config-category-item")?.classList.toggle(
        "is-active",
        button.dataset.category === category,
      );
    });

    title.textContent = `${category} 하위 직무`;
    detail.querySelectorAll(".config-row:not(.config-row--head)")
      .forEach((row) => row.remove());
    detail.insertAdjacentHTML(
      "beforeend",
      categoryData[category]
        .map((job) => createRow(job, category))
        .join(""),
    );
  };

  categoryList.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-category]");

    if (button) renderCategory(button.dataset.category);
  });
});
