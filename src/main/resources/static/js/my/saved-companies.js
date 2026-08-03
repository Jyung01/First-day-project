document.addEventListener("DOMContentLoaded", function () {
    // 탭 메뉴
    const tabs = document.querySelectorAll(".job-tab");
    const cards = document.querySelectorAll(".saved-job-card");

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            // active 변경
            tabs.forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");

            const status = tab.dataset.status;

            cards.forEach((card) => {
                if (status === "all") {
                    card.classList.remove("hide");
                } else {
                    if (card.dataset.status === status) {
                        card.classList.remove("hide");
                    } else {
                        card.classList.add("hide");
                    }
                }
            });
        });
    });

    // 공고 좋아요 기능
    document.querySelectorAll(".favorite-btn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            // 관심 안 된 상태
            if (!btn.classList.contains("active")) {
                btn.classList.add("active");

                // 스프링에서는 여기서 관심등록 API 호출
                // fetch(...)

                return;
            }

            // 관심된 상태 -> 해제 모달
            showConfirmModal({
                iconClass: "info",
                iconHtml: '<i class="fa-solid fa-exclamation"></i>',

                title: "관심 기업에서 해제할까요?",
                message:
                    "선택한 기업을 관심 기업 목록에서 삭제합니다.\n목록에서 즉시 사라집니다.",

                leftText: "취소",
                rightText: "관심 해제",

                leftClass: "btn-outline",

                onRight: async () => {
                    btn.classList.remove("active");

                    // 관심공고 페이지라면 카드 제거
                    // btn.closest(".saved-job-card")?.remove();

                    // 스프링에서는 여기서 fetch 호출
                    // await fetch(`/my/saved-jobs/${jobId}`, {
                    //     method: "DELETE",
                    // });
                    //
                },
            });
        });
    });
    // document.querySelectorAll(".favorite-btn").forEach((btn) => {
    //     btn.addEventListener("click", () => {
    //         btn.classList.toggle("active");
    //     });
    // });
});