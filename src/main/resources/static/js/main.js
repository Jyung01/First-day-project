function initializeRedesignPage() {
    const tabs = document.querySelectorAll(".today-tab");
    const lists = document.querySelectorAll("[data-job-list-content]");

    initializeHeroCopyFade();
    initializeMainBanner();

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            const target = tab.dataset.jobList;
            tabs.forEach((item) => item.classList.toggle("active", item === tab));
            lists.forEach((list) => {
                list.hidden = list.dataset.jobListContent !== target;
            });
        });
    });

    prepareReveal(document.querySelectorAll(".tool-card"));
    prepareReveal(document.querySelectorAll(".category-list > a"), 110);
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
                        <a href="/my/profile-edit">희망 직무 설정하기 <span class="main-arrow" aria-hidden="true"></span></a>
                    </div>`;
                return;
            }
            aiList.innerHTML = data.jobs.slice(0, 3).map((job) => recommendationRow(
                job,
                data.matchScores?.[job.jobPostingId],
                data.matchReasons?.[job.jobPostingId]
            )).join("");
            prepareReveal(aiList.querySelectorAll(".ai-job-row"));
        })
        .catch((error) => {
            aiList.innerHTML = `
                <div class="ai-empty-state">
                    <strong>${escapeHtml(error.message)}</strong>
                    <a href="/job/list">전체 공고 보기 <span class="main-arrow" aria-hidden="true"></span></a>
                </div>`;
        });
}

function initializeMainBanner() {
    const banner = document.querySelector("[data-main-banner]");
    if (!banner) return;
    if (typeof Swiper === "undefined") return;

    const slideCount = banner.querySelectorAll(".swiper-slide").length;
    new Swiper(banner, {
        slidesPerView: 1,
        spaceBetween: 0,
        loop: slideCount > 1,
        speed: 650,
        autoplay: slideCount > 1 ? {
            delay: 5000,
            disableOnInteraction: false,
            pauseOnMouseEnter: true
        } : false,
        pagination: {
            el: banner.querySelector(".swiper-pagination"),
            clickable: true
        },
        navigation: {
            prevEl: banner.querySelector(".main-banner__arrow--prev"),
            nextEl: banner.querySelector(".main-banner__arrow--next")
        },
        keyboard: {
            enabled: true
        }
    });
}

function initializeHeroCopyFade() {
    const copy = document.querySelector(".hero-copy");
    const copyContent = copy?.querySelector(".redesign-wrap");
    const media = document.querySelector(".hero-media");
    if (!copy || !copyContent || !media) return;

    const mobileViewport = window.matchMedia("(max-width: 900px)");
    let frameId = 0;
    const updateOpacity = () => {
        if (mobileViewport.matches) {
            copyContent.style.removeProperty("opacity");
            frameId = 0;
            return;
        }

        const copyBottom = copyContent.getBoundingClientRect().bottom;
        const mediaTop = media.getBoundingClientRect().top;
        const distance = mediaTop - copyBottom;
        const fadeStart = 40;
        const fadeEnd = -80;
        const opacity = Math.max(0, Math.min(1,
            (distance - fadeEnd) / (fadeStart - fadeEnd)
        ));
        copyContent.style.opacity = opacity.toFixed(3);
        frameId = 0;
    };

    const requestUpdate = () => {
        if (!frameId) frameId = window.requestAnimationFrame(updateOpacity);
    };

    window.addEventListener("scroll", requestUpdate, { passive: true });
    window.addEventListener("resize", requestUpdate);
    mobileViewport.addEventListener("change", requestUpdate);
    requestUpdate();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initializeRedesignPage, { once: true });
} else {
    initializeRedesignPage();
}

function prepareReveal(elements, interval = 170) {
    if (!elements.length) return;

    const observer = new IntersectionObserver((entries, currentObserver) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            currentObserver.unobserve(entry.target);
            window.setTimeout(() => entry.target.classList.add("is-visible"), Number(entry.target.dataset.revealDelay));
        });
    }, { threshold: .16 });

    elements.forEach((element, index) => {
        element.classList.add("reveal-item");
        element.dataset.revealDelay = String(120 + index * interval);
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => observer.observe(element));
        });
    });
}

function recommendationRow(job, score, reasons) {
    const companyLogo = job.logoUrl
        ? `<img src="${escapeHtml(job.logoUrl)}" loading="lazy" decoding="async" alt="${escapeHtml(job.companyName || "기업")} 로고">`
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
            <span class="ai-arrow main-arrow" aria-hidden="true"></span>
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
