function initializeRedesignPage() {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const tabs = document.querySelectorAll(".today-tab");
    const lists = document.querySelectorAll("[data-job-list-content]");

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            const target = tab.dataset.jobList;
            tabs.forEach((item) => item.classList.toggle("active", item === tab));
            lists.forEach((list) => {
                list.hidden = list.dataset.jobListContent !== target;
            });
        });
    });

    prepareReveal(document.querySelectorAll(".tool-card"), reduceMotion);
    const aiList = document.getElementById("redesignAiList");
    if (!aiList || aiList.dataset.personalMember !== "true") return;

    fetch("/api/main/recommendations")
        .then(async (response) => {
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "추천 공고를 불러오지 못했습니다.");
            return data;
        })
        .then((data) => {
            if (!data.jobs?.length) {
                aiList.innerHTML = `
                    <div class="ai-empty-state">
                        <strong>추천할 공고가 아직 없어요.</strong>
                        <a href="/my/profile-edit">희망 직무 설정하기 →</a>
                    </div>`;
                return;
            }
            aiList.innerHTML = data.jobs.slice(0, 3).map((job) => recommendationRow(
                job,
                data.matchScores?.[job.jobPostingId],
                data.matchReasons?.[job.jobPostingId]
            )).join("");
            prepareReveal(aiList.querySelectorAll(".ai-job-row"), reduceMotion);
        })
        .catch((error) => {
            aiList.innerHTML = `
                <div class="ai-empty-state">
                    <strong>${escapeHtml(error.message)}</strong>
                    <a href="/job/list">전체 공고 보기 →</a>
                </div>`;
        });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeRedesignPage, { once: true });
} else {
    initializeRedesignPage();
}

function prepareReveal(elements, reduceMotion) {
    if (!elements.length || reduceMotion) return;

    const observer = new IntersectionObserver((entries, currentObserver) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            currentObserver.unobserve(entry.target);
            window.setTimeout(() => entry.target.classList.add("is-visible"), Number(entry.target.dataset.revealDelay));
        });
    }, { threshold: .16 });

    elements.forEach((element, index) => {
        element.classList.add("reveal-item");
        element.dataset.revealDelay = String(120 + index * 170);
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => observer.observe(element));
        });
    });
}

function recommendationRow(job, score, reasons) {
    const companyLogo = job.logoUrl
        ? `<img src="${escapeHtml(job.logoUrl)}" alt="${escapeHtml(job.companyName || "기업")} 로고">`
        : `<span class="ai-company-logo-fallback" aria-hidden="true">▦</span>`;
    const reason = Array.isArray(reasons) && reasons.length
        ? reasons[0]
        : "희망 직무와 관련 있는 공고예요.";
    const match = score ? `AI ${Math.min(99, Math.max(70, score))}%` : "AI 추천";

    return `
        <a class="ai-job-row" href="/job/detail?jobPostingId=${encodeURIComponent(job.jobPostingId)}">
            <span class="ai-company-mark">${companyLogo}</span>
            <span class="ai-job-info">
                <strong>${escapeHtml(job.title || "채용공고")}</strong>
                <span>${escapeHtml(job.companyName || "기업") } · ${escapeHtml(job.workRegion || "지역 미정")}</span>
            </span>
            <em class="ai-score">${match}</em>
            <span class="ai-reason">${escapeHtml(reason)}</span>
            <span class="ai-arrow">→</span>
        </a>`;
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "'": "&#39;",
        "\"": "&quot;"
    })[character]);
}
