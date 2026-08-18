document.addEventListener("DOMContentLoaded", function () {
    // 배너 스와이퍼
    const bannerElement = document.querySelector(".bannerSwiper");
    if (bannerElement && typeof Swiper !== "undefined") {
      const slideCount = Number(bannerElement.dataset.slideCount || 0);
      new Swiper(bannerElement, {
        loop: slideCount > 1,

        autoplay: slideCount > 1 ? {
            delay: 3000,
            disableOnInteraction: false,
        } : false,

        pagination: {
            el: ".swiper-pagination",
            clickable: true,
        },

        navigation: {
            nextEl: ".swiper-button-next",
            prevEl: ".swiper-button-prev",
        },
      });
    }

    // AI 추천은 메인 로딩을 막지 않고 개인회원이 탭을 누를 때만 가져온다.
    const personalizedGrid = document.querySelector(".personalized-grid");
    const popularGrid = document.querySelector(".popular-grid");
    const latestGrid = document.querySelector(".latest-grid");

    let recommendationsLoaded = false;
    let recommendationsLoading = false;
    let selectedTab = "popular";
    let recommendationMarkup = "";

    document.querySelectorAll(".tab").forEach(tab => {
        tab.addEventListener("click", () => {
            selectedTab = tab.dataset.tab;
            document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");

            if (selectedTab === "personalized") {
                if (recommendationsLoaded) {
                    showRecommendations();
                } else {
                    loadRecommendations(tab);
                }
                return;
            }

            hideAllJobGrids();
            (selectedTab === "popular" ? popularGrid : latestGrid).style.display = "grid";
        });
    });

    async function loadRecommendations() {
        if (recommendationsLoaded || recommendationsLoading || !personalizedGrid) return;
        recommendationsLoading = true;
        const recommendationTab = document.querySelector('.ai-recommendation-tab');
        recommendationTab?.classList.add('is-loading');
        const skeletonTimer = window.setTimeout(() => {
            if (selectedTab === "personalized" && !recommendationsLoaded) {
                hideAllJobGrids();
                personalizedGrid.innerHTML = skeletonCards();
                personalizedGrid.style.display = "grid";
            }
        }, 300);
        try {
            const response = await fetch('/api/main/recommendations');
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || '추천을 불러오지 못했습니다.');
            recommendationsLoaded = true;
            recommendationMarkup = data.jobs.length
                ? data.jobs.map(job => recommendationCard(job, data.matchScores, data.matchReasons)).join('')
                : '<div class="recommendation-empty"><strong>희망 직무와 일치하는 모집 중 공고가 아직 없어요.</strong><p>희망 직무를 수정하면 더 다양한 공고를 추천해 드립니다.</p><a class="recommendation-link" href="/my/profile-edit">희망 직무 설정하기 →</a></div>';
            if (selectedTab === "personalized") showRecommendations();
        } catch (error) {
            if (selectedTab === "personalized") {
                hideAllJobGrids();
                personalizedGrid.innerHTML = `<div class="recommendation-empty"><strong>${escapeHtml(error.message)}</strong><p>잠시 후 다시 시도해주세요.</p></div>`;
                personalizedGrid.style.display = "grid";
            }
        } finally {
            window.clearTimeout(skeletonTimer);
            recommendationsLoading = false;
            recommendationTab?.classList.remove('is-loading');
        }
    }

    function showRecommendations() {
        hideAllJobGrids();
        personalizedGrid.innerHTML = recommendationMarkup;
        personalizedGrid.style.display = "grid";
    }

    function hideAllJobGrids() {
        personalizedGrid.style.display = "none";
        popularGrid.style.display = "none";
        latestGrid.style.display = "none";
    }

    function skeletonCards() {
        return Array.from({ length: 3 }, () => '<div class="job-card recommendation-skeleton" aria-hidden="true"><div class="skeleton-logo"></div><div class="skeleton-line skeleton-company"></div><div class="skeleton-line skeleton-title"></div><div class="skeleton-line skeleton-title short"></div><div class="skeleton-line skeleton-detail"></div><div class="skeleton-tags"><span></span><span></span></div></div>').join('');
    }

    function recommendationCard(job, scores, reasons) {
        const score = scores[String(job.jobPostingId)];
        const points = (reasons[String(job.jobPostingId)] || []).map(escapeHtml).join(' · ');
        return `<a href="/job/detail?jobPostingId=${encodeURIComponent(job.jobPostingId)}" class="job-card">
            <div class="co-row"><img class="co-logo" src="${escapeHtml(job.logoUrl || '')}" alt="회사 로고"><div class="co-name">${escapeHtml(job.companyName)}</div></div>
            <div class="job-title">${escapeHtml(job.title)}</div>
            ${score == null ? '' : `<div class="ai-match"><span class="ai-match-mark">AI</span><span>추천 매칭</span><strong>${score}%</strong></div>`}
            ${points ? `<div class="ai-reasons"><div class="ai-reasons-title">매칭 포인트</div><div class="ai-reason-list"><span class="ai-reason">${points}</span></div></div>` : ''}
            <div class="job-loc">${escapeHtml(job.workRegion)} · ${escapeHtml(job.careerType)}</div>
            <div class="tag-row"><span class="tag">${escapeHtml(job.employmentType)}</span><span class="tag">${escapeHtml(job.categoryName)}</span></div></a>`;
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value).replace(/[&<>'"]/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[character]));
    }
})
