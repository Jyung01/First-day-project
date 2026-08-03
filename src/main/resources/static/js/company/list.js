document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".wish-btn").forEach((btn) => {

        btn.addEventListener("click", (e) => {
            e.preventDefault();

            // TODO
            // fetch('/company/wish')

            btn.classList.toggle("active");

            const icon = btn.querySelector("i");

            icon.classList.toggle("fa-regular");
            icon.classList.toggle("fa-solid");

            /*
           ================================
           Spring Controller 연동 예정
           ================================

           fetch(`/company/bookmark/${companyId}`, {
               method: "POST"
           })
           .then(response => response.json())
           .then(data => {
               console.log(data);
           })
           .catch(error => {

               console.error(error);

               // 실패 시 원상복구
               button.classList.toggle("active");

               if (button.classList.contains("active")) {
                   icon.classList.remove("fa-regular");
                   icon.classList.add("fa-solid");
               } else {
                   icon.classList.remove("fa-solid");
                   icon.classList.add("fa-regular");
               }

           });

           */
        });

    });

});
