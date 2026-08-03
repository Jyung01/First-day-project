document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".write-review-btn").forEach((btn) => {
        btn.addEventListener("click", (e) => {
            // 기본 이동 막기
            e.preventDefault();

            // 1. TODO : 로그인 여부 확인
            // const isLogin = false;
            //
            // if (!isLogin) {
            //     showConfirmModal({
            //         iconClass: "info",
            //         iconHtml: '<i class="fa-solid fa-lock"></i>',
            //
            //         title: "로그인이 필요합니다.",
            //         message:
            //             "기업리뷰와 면접후기는\n로그인 후 작성할 수 있습니다.",
            //
            //         leftText: "취소",
            //         rightText: "로그인",
            //
            //         leftClass: "btn-outline",
            //
            //         onRight: () => {
            //             location.href = "/member/login";
            //         },
            //
            //     });
            //
            //     return;
            // }


            // 2. TODO : 작성 권한 확인
            // const result = await fetch(...);
            // const hasPermission = false;
            //
            // if (!hasPermission) {
            //     showConfirmModal({
            //         iconClass: "info",
            //         iconHtml: '<i class="fa-solid fa-exclamation"></i>',
            //
            //         title: "작성 권한이 없습니다.",
            //         message:
            //             "입사 완료 또는 면접 완료 이력이\n확인된 회원만 후기를 작성할 수 있습니다.",
            //
            //         leftText: "확인",
            //         leftClass: "btn-primary",
            //
            //         rightText: "확인",
            //
            //         onRight: () => {}
            //     });
            //     // 오른쪽 버튼 숨김
            //     document.getElementById("rightBtn").style.display = "none";
            //
            //     // 버튼을 가운데로
            //     document.getElementById("leftBtn").style.gridColumn = "1/-1";
            //
            //     return;
            // }


            // 3. TODO : 권한 없으면 모달
            // if (!result.hasPermission) -> 작성 권한 없음 모달

            // 4. TODO : 권한 있으면 작성 페이지 이동
            location.href = "/company/review/write";
        });
    })

    /* ===========================
        리뷰 등록 완료 모달
    =========================== */
    document.querySelectorAll(".btn-submit").forEach((btn) => {

        btn.addEventListener("click", (e) => {

            e.preventDefault();

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



