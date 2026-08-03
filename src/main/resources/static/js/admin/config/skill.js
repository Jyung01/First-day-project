document.addEventListener("DOMContentLoaded", () => {
  const categoryList = document.querySelector("[data-skill-category-list]");
  const detail = document.querySelector("[data-skill-detail]");
  const title = document.querySelector("[data-skill-title]");

  if (!categoryList || !detail || !title) return;

  const skillData = {
    언어: [
      ["Java", 328],
      ["Python", 286],
      ["JavaScript", 251],
      ["TypeScript", 194],
      ["Kotlin", 103],
      ["Go", 81],
    ],
    프레임워크: [
      ["Spring Boot", 241],
      ["React", 218],
      ["Vue.js", 126],
      ["Django", 84],
      ["Next.js", 79],
    ],
    데이터베이스: [
      ["MySQL", 274],
      ["PostgreSQL", 172],
      ["Oracle", 108],
      ["MongoDB", 94],
      ["Redis", 87],
    ],
    "클라우드·DevOps": [
      ["AWS", 263],
      ["Docker", 191],
      ["Kubernetes", 105],
      ["Jenkins", 88],
      ["GitHub Actions", 72],
    ],
    기타: [
      ["Git", 317],
      ["Figma", 122],
      ["Jira", 94],
      ["Notion", 81],
    ],
  };

  const escapeHtml = (value) =>
    value.replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");

  const createRow = ([name, count]) => {
    const safeName = escapeHtml(name);

    return `
      <div class="config-row">
        <strong>${safeName}</strong>
        <span>${count}건</span>
        <div class="config-actions">
          <button
            class="table-action"
            data-modal-open="skill"
            data-modal-title="기술 스택 수정"
            data-modal-value="${safeName}"
          >수정</button>
          <button
            class="table-action"
            data-modal-open="skill-delete"
            data-modal-value="${safeName}"
          >삭제</button>
        </div>
      </div>
    `;
  };

  const renderSkills = (category) => {
    categoryList.querySelectorAll("button").forEach((button) => {
      button.classList.toggle(
        "is-active",
        button.dataset.skillCategory === category,
      );
    });

    title.textContent = `${category} 기술 스택`;
    detail.querySelectorAll(".config-row:not(.config-row--head)")
      .forEach((row) => row.remove());
    detail.insertAdjacentHTML(
      "beforeend",
      skillData[category].map(createRow).join(""),
    );
  };

  categoryList.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-skill-category]");

    if (button) renderSkills(button.dataset.skillCategory);
  });
});
