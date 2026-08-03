document.addEventListener("DOMContentLoaded", () => {

    const mySalaryBtn = document.querySelector(".my-salary-btn");

    if (!mySalaryBtn) return;

    mySalaryBtn.addEventListener("click", (e) => {

        e.preventDefault();

        // ===============================
        // TODO : 백엔드 연결
        // 로그인 여부 확인
        //
        // 예시)
        // if (isLogin) {
        //     location.href = "/salary/register";
        //     return;
        // }
        //
        // 현재는 UI 확인을 위해
        // 항상 로그인 모달을 띄운다.
        // ===============================

        showConfirmModal({

            iconClass: "info",
            iconHtml: "!",

            title: "로그인이 필요합니다",

            message:
                "연봉정보 등록은 개인회원 로그인 후 이용할 수 있습니다.\n로그인하면 작성하던 연봉정보 등록 화면으로 돌아옵니다.",

            extraHtml: `
                <div class="modal-guide">
                    로그인 → 로그인 페이지로 이동 · 완료 후 연봉정보 등록 화면으로 복귀
                </div>
            `,

            leftText: "닫기",
            rightText: "로그인",

            leftClass: "btn-outline",
            rightClass: "btn-primary",

            onRight() {

                // ===============================
                // TODO : 백엔드 연결
                // 로그인 페이지로 이동
                //
                // location.href = "/member/login";
                // ===============================

                console.log("로그인 페이지 이동");

            }

        });

    });

});