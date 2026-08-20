document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".wish-btn").forEach((btn) => {

        btn.addEventListener("click", (e) => {
            e.preventDefault();

            btn.classList.toggle("active");

            const icon = btn.querySelector("i");

            icon.classList.toggle("fa-regular");
            icon.classList.toggle("fa-solid");

            const companyId = btn.dataset.companyId;

            fetch(`/company/wish/${companyId}`, {
                method: "POST"
            })
                .then(response => {

                    if (!response.ok) {
                        throw new Error("관심기업 처리 실패");
                    }

                    return response.json();
                })
                .then(data => {

                    console.log(data);

                    // 로그인 안 된 경우
                    if (!data.success) {
                        alert(data.message);
                        return;
                    }

                    if (data.wished) {

                        btn.classList.add("active");

                        icon.classList.remove("fa-regular");
                        icon.classList.add("fa-solid");

                    } else {

                        btn.classList.remove("active");

                        icon.classList.remove("fa-solid");
                        icon.classList.add("fa-regular");
                    }

                })
                .catch(error => {
                    console.error(error);
                });


        });

    });

});
