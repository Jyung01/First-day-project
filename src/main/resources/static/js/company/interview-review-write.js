document.addEventListener("DOMContentLoaded", function () {
    // 난이도 버튼 선택 토글
    const diffButtons = document.querySelectorAll('.diff-btn');
    const hiddenInput = document.getElementById('selectedDifficulty');

    diffButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            diffButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            hiddenInput.value = this.textContent;
        });
    });
})