document.addEventListener("DOMContentLoaded", () => {

    const deleteBtn = document.querySelector(".delete-btn");

    deleteBtn.addEventListener("click", (e) => {

        e.preventDefault();

        showConfirmModal({

            iconClass: "info",
            iconHtml: "!",
            title: "연봉정보를 삭제할까요?",

            message:
                "삭제 후에는 복구할 수 없으며 해당 정보는 익명 집계에서도 제외됩니다.",



            leftText: "취소",
            rightText: "삭제",

            leftClass: "btn-outline",
            rightClass: "btn-danger",

            onRight() {

                // TODO : 삭제 API 호출

                /*
                fetch("/salary/delete", {
                    method: "DELETE"
                })
                .then(...)
                */

            }

        });

    });

});