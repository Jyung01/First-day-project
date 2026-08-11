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
    const popularGrid = document.querySelector(".popular-grid");
    const latestGrid = document.querySelector(".latest-grid");

    document.querySelectorAll(".tab:not(.locked)").forEach(tab => {
        tab.addEventListener("click", () => {

            document.querySelectorAll(".tab:not(.locked)")
                .forEach(t => t.classList.remove("active"));

            tab.classList.add("active");

            if (tab.dataset.tab === "popular") {
                popularGrid.style.display = "grid";
                latestGrid.style.display = "none";
            } else {
                popularGrid.style.display = "none";
                latestGrid.style.display = "grid";
            }
        });
    });
})
