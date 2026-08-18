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

    // 탭 전환 (인기 공고 / 최신 공고, AI 추천은 잠금 표시)
    const personalizedGrid = document.querySelector(".personalized-grid");
    const popularGrid = document.querySelector(".popular-grid");
    const latestGrid = document.querySelector(".latest-grid");

    document.querySelectorAll(".tab").forEach(tab => {
        tab.addEventListener("click", () => {

            document.querySelectorAll(".tab")
                .forEach(t => t.classList.remove("active"));

            tab.classList.add("active");

            personalizedGrid.style.display = "none";
            popularGrid.style.display = "none";
            latestGrid.style.display = "none";

            if (tab.dataset.tab === "personalized") {
                personalizedGrid.style.display = "grid";
            } else if (tab.dataset.tab === "popular") {
                popularGrid.style.display = "grid";
            } else {
                latestGrid.style.display = "grid";
            }
        });
    });
})
