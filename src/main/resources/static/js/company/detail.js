document.addEventListener("DOMContentLoaded", () => {

    const wishButtons = document.querySelectorAll(".wish-btn");

    wishButtons.forEach(button => {

        button.addEventListener("click", () => {

            const companyNo = button.dataset.companyId;
            const icon = button.querySelector("i");

            const isFavorite = button.classList.toggle("active");

            if (isFavorite) {
                icon.classList.remove("fa-regular");
                icon.classList.add("fa-solid");
            } else {
                icon.classList.remove("fa-solid");
                icon.classList.add("fa-regular");
            }

            /*
            ========================================
            TODO : 관심기업 등록/취소 Controller 연결
            ========================================

            fetch("/company/favorite", {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({

                    companyNo: companyNo

                })

            })
            .then(response => response.json())
            .then(data => {

                // 성공 시 아무 작업 없음
                // DB 상태에 따라 하트 유지

            })
            .catch(error => {

                console.error(error);

                // 실패 시 UI 원상복구
                button.classList.toggle("active");

                if(button.classList.contains("active")){

                    icon.classList.remove("fa-regular");
                    icon.classList.add("fa-solid");

                }else{

                    icon.classList.remove("fa-solid");
                    icon.classList.add("fa-regular");

                }

            });

            */

        });

    });

});