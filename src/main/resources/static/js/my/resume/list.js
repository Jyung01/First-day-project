document.addEventListener("DOMContentLoaded", function () {
    const resumeList = document.querySelector("[data-resume-list]");
    const resumeEmpty = document.querySelector("[data-resume-empty]");
    const deleteButtons = document.querySelectorAll("[data-resume-delete]");

    if (!resumeList) {
        return;
    }

    function updateEmptyState() {
        const resumeCards = resumeList.querySelectorAll("[data-resume-id]");

        const isEmpty = resumeCards.length === 0;

        resumeList.hidden = isEmpty;

        if (resumeEmpty) {
            resumeEmpty.hidden = !isEmpty;
        }
    }

    deleteButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            const resumeId = button.dataset.resumeDelete;
            const resumeTitle = button.dataset.resumeTitle || "선택한 이력서";

            showConfirmModal({
                iconClass: "danger",
                iconHtml: "!",
                title: "이력서를 삭제할까요?",
                message: `"${resumeTitle}"을 삭제합니다.\n` + "삭제한 이력서는 다시 복구할 수 없습니다.",
                leftText: "취소",
                rightText: "삭제",
                leftClass: "btn-outline",
                rightClass: "btn-danger",

                onRight: function () {
                    /*
                     * 실제 기능 구현 시 삭제 API로 교체
                     *
                     * 예시:
                     *
                     * fetch(`/my/resume/${resumeId}`, {
                     *     method: "DELETE"
                     * })
                     *     .then(function (response) {
                     *         if (!response.ok) {
                     *             throw new Error(
                     *                 "이력서 삭제에 실패했습니다."
                     *             );
                     *         }
                     *
                     *         return response.json();
                     *     })
                     *     .then(function () {
                     *         removeResumeCard(resumeId);
                     *     })
                     *     .catch(function (error) {
                     *         console.error(error);
                     *     });
                     */

                    // 현재 화면 테스트용
                    removeResumeCard(resumeId);
                },
            });
        });
    });

    function removeResumeCard(resumeId) {
        const resumeCard = resumeList.querySelector(`[data-resume-id="${resumeId}"]`);

        resumeCard?.remove();
        updateEmptyState();
    }

    updateEmptyState();
});
