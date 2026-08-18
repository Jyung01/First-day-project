document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".write-review-btn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            const companyId = new URLSearchParams(location.search).get("companyId");
            const writePath = location.pathname.includes("interview")
                ? "/company/interview-review/write"
                : "/company/review/write";
            location.href = `${writePath}?companyId=${companyId}`;
        });
    })

    /* ===========================
        리뷰 등록 완료 모달
    =========================== */
    document.querySelectorAll(".btn-submit").forEach((btn) => {

        btn.addEventListener("click", (e) => {

            if (btn.closest("form")) {
                return;
            }

            // TODO : 유효성 검사
            // TODO : fetch 저장

            showConfirmModal({
                iconClass: "success",
                iconHtml: '<i class="fa-solid fa-check"></i>',

                title: "후기가 등록되었습니다.",
                message:
                    "작성한 후기는 등록 즉시 공개되며\n마이페이지에서 확인하고 수정할 수 있습니다.",

                leftText: "닫기",
                rightText: "내 후기 보기",

                leftClass: "btn-outline",

                // 닫기
                onLeft: () => {
                    if (location.pathname.includes("interview")) {
                        // 면접 후기 목록
                        location.href = "/company/interview-reviews";

                        // 기능 구현 후 아래처럼 사용하기
                        // const companyId = new URLSearchParams(location.search).get("companyId");
                        // location.href = `/company/review?companyId=${companyId}`;
                    } else {
                        // 기업 리뷰 목록
                        location.href = "/company/reviews";
                    }
                },

                // 내 후기 보기
                onRight: () => {
                    // TODO : 내 후기 페이지 경로로 변경
                    location.href = "/my/reviews";
                }
            });
           
        });

    });

});



