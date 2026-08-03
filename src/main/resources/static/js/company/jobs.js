document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".bookmark-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            btn.classList.toggle("active");

            const icon = btn.querySelector("i");

            if (btn.classList.contains("active")) {
                icon.classList.remove("fa-regular");
                icon.classList.add("fa-solid");
            } else {
                icon.classList.remove("fa-solid");
                icon.classList.add("fa-regular");
            }

            // TODO : 관심공고 등록/해제 API 호출
            // fetch(...)
        });
    });
})