/**
 * 정적 HTML 미리보기 전용 공통 include 스크립트
 * Spring Boot + Thymeleaf에서는 th:replace가 대신 처리합니다.
 */
async function loadInclude(element) {
  const path = element.dataset.include;

  if (!path) return;

  try {
    const response = await fetch(path);

    if (!response.ok) {
      throw new Error(`${response.status} ${response.statusText}`);
    }

    element.innerHTML = await response.text();
  } catch (error) {
    console.error(`[include] ${path} 로드 실패`, error);
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const targets = [...document.querySelectorAll('[data-include]')];
  await Promise.all(targets.map(loadInclude));
  document.dispatchEvent(new CustomEvent('includes:loaded'));
});
