document.addEventListener("DOMContentLoaded", () => {

    const submitBtn = document.querySelector(".submit-btn");

    submitBtn.addEventListener("click", (e) => {

        e.preventDefault();

        showConfirmModal({

            iconClass: "info",
            iconHtml: '<i class="fa-solid fa-question"></i>',

            title: "입력한 연봉정보를 등록할까요?",

            message: "입력 내용과 본인이 실제 근무한 기업인지 다시 확인해주세요.",

            extraHtml: `
        <div class="salary-summary">

            <div class="summary-row">
                <span>기업</span>
                <strong>네이버클라우드</strong>
            </div>

            <div class="summary-row">
                <span>직무 · 경력</span>
                <strong>백엔드 개발 · 3년</strong>
            </div>

            <div class="summary-row">
                <span>세전 연봉</span>
                <strong class="point">5,200만원</strong>

                <span>기준연도</span>
                <strong>2026년</strong>
            </div>

        </div>

        <p class="modal-desc">
            등록 후 내 연봉정보 목록에서 수정할 수 있으며
            개별 금액은 공개되지 않습니다.
        </p>
    `,

            leftText: "취소",
            rightText: "등록하기",

            leftClass: "btn-outline",
            rightClass: "btn-primary",

            onRight() {

                const form = document.querySelector(".salary-form");

                const formData = new FormData(form);

                fetch("/salary/register", {      // TODO : 등록 API 주소 변경
                    method: "POST",
                    body: formData
                })
                    .then(response => {

                        if (!response.ok) {
                            throw new Error("등록 실패");
                        }

                        return response.json();      // TODO : 응답 형식에 맞게 수정

                    })
                    .then(data => {

                        showConfirmModal({
                            iconClass: "success",
                            iconHtml: '<i class="fa-solid fa-check"></i>',

                            title: "연봉정보가 등록되었습니다.",

                            message:
                                "소중한 정보를 등록해주셔서 감사합니다.\n조건별 데이터가 3건 이상 모이면 익명 평균값에 반영됩니다.",

                            extraHtml: `
    <div class="modal-guide">
        <strong>상세 보기</strong>&nbsp;→&nbsp;등록한 기업의 연봉 상세 화면으로 이동
    </div>
`,

                            leftText: "닫기",
                            rightText: "기업별 연봉 상세 보기",

                            leftClass: "btn-outline",
                            rightClass: "btn-primary",

                            onRight() {

                                location.href = "/salary/detail/1"; // TODO : 등록한 기업 상세페이지

                            }

                        });

                    })
                    .catch(error => {

                        console.error(error);

                        showConfirmModal({

                            iconClass: "danger",
                            iconHtml: "!",

                            title: "등록에 실패했습니다.",

                            message:
                                "잠시 후 다시 시도해주세요.",

                            rightText: "확인",
                            rightClass: "btn-primary"

                        });

                    });

            }

        });

    });

});