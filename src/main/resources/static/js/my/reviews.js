document.addEventListener("DOMContentLoaded", function () {
    // 탭 메뉴
    const tabs = document.querySelectorAll(".review-tab");
    const companyCards = document.querySelectorAll(".company-review");
    const interviewCards = document.querySelectorAll(".interview-review");

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");

            if (tab.dataset.type === "company") {
                companyCards.forEach((c) => (c.style.display = "block"));
                interviewCards.forEach((c) => (c.style.display = "none"));
            } else {
                companyCards.forEach((c) => (c.style.display = "none"));
                interviewCards.forEach((c) => (c.style.display = "block"));
            }
        });
    });

    // 삭제 모달
    document.querySelectorAll(".btn-delete").forEach((btn) => {

        btn.addEventListener("click", () => {

            showConfirmModal({
                iconClass: "danger",
                iconHtml: '<i class="fa-solid fa-exclamation"></i>',

                title: "후기를 삭제할까요?",
                message:
                    "삭제한 후기는 복구할 수 없습니다.\n삭제 후 다시 작성해야 합니다.",

                leftText: "취소",
                rightText: "삭제",

                leftClass: "btn-outline",
                rightClass: "btn-danger",

                onRight: async () => {

                    // TODO
                    // const reviewId = btn.dataset.reviewId;

                    // await fetch(`/my/reviews/${reviewId}`, {
                    //     method: "DELETE",
                    // });

                    btn.closest(".review-card")?.remove();

                },
            });

        });

    });
});