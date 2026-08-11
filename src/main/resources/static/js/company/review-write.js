document.addEventListener("DOMContentLoaded", function () {
    // 별점 등록
    document.querySelectorAll(".star-rating").forEach((rating) => {

        const stars = rating.querySelectorAll("i");
        const text = rating.nextElementSibling;
        const target = rating.dataset.target;
        const input = document.querySelector(`input[name="${target}"]`);

        if (input && !input.value) {
            input.value = "5";
        }

        stars.forEach((star, index) => {

            star.addEventListener("click", () => {

                stars.forEach((s, i) => {
                    s.classList.toggle("active", i <= index);
                });

                text.textContent = `${index + 1}점`;
                if (input) {
                    input.value = String(index + 1);
                }

                // 별점 평균 구하기
                const scores = [...document.querySelectorAll(".rating-val")]
                    .map(el => Number(el.textContent.replace("점", "")));

                const avg = (
                    scores.reduce((a, b) => a + b, 0) / scores.length
                ).toFixed(1);

                document.getElementById("avgScore").textContent = avg;

            });

        });

    });



    document.getElementById('companyReviewForm').addEventListener('submit', function (e) {
        // 유효성 검사 로직 작성 위치
    });
})
