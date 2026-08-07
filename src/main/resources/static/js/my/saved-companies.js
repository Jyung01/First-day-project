document.addEventListener("DOMContentLoaded", () => {
    document.querySelector(".company-sort")?.addEventListener("change", () => {
        document.getElementById("companySortForm").submit();
    });

    document.querySelectorAll(".favorite-btn").forEach((button) => {
        button.addEventListener("click", () => {
            showConfirmModal({
                iconClass: "info",
                iconHtml: '<i class="fa-solid fa-heart-crack"></i>',
                title: "관심 기업에서 해제할까요?",
                message: "선택한 기업이 관심 기업 목록에서 제거됩니다.",
                leftText: "취소",
                rightText: "관심 해제",
                leftClass: "btn-outline",
                rightClass: "btn-danger",
                onRight: async () => {
                    button.disabled = true;
                    try {
                        const response = await fetch("/my/saved-companies/remove", {
                            method: "POST",
                            headers: {"Content-Type": "application/x-www-form-urlencoded"},
                            body: new URLSearchParams({companyId: button.dataset.companyId})
                        });
                        const result = await response.json();
                        if (!response.ok) return showCompanyResult(false, result.message || "관심 기업 해제에 실패했습니다.");
                        showCompanyResult(true, result.message);
                    } catch (error) {
                        showCompanyResult(false, "관심 기업 해제 중 오류가 발생했습니다.");
                    } finally {
                        button.disabled = false;
                    }
                }
            });
        });
    });
});

function showCompanyResult(success, message) {
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        iconHtml: success ? '<i class="fa-solid fa-check"></i>' : '<i class="fa-solid fa-exclamation"></i>',
        title: success ? "관심 기업에서 해제되었습니다" : "처리할 수 없습니다",
        message,
        leftVisible: false,
        rightText: "확인",
        rightClass: "btn-primary",
        onLeft: success ? () => location.reload() : null,
        onRight: success ? () => location.reload() : null
    });
}
