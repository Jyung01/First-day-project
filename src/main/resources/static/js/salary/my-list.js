document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".delete-btn").forEach((button) => {
        button.addEventListener("click", () => {
            showConfirmModal({
                iconClass: "info",
                iconHtml: "!",
                title: "연봉정보를 삭제할까요?",
                message: "삭제한 정보는 연봉 통계에서 즉시 제외됩니다.",
                leftText: "취소",
                rightText: "삭제",
                leftClass: "btn-outline",
                rightClass: "btn-danger",
                onRight: async () => {
                    button.disabled = true;
                    try {
                        const response = await fetch("/salary/delete", {
                            method: "POST",
                            headers: {"Content-Type": "application/x-www-form-urlencoded"},
                            body: new URLSearchParams({salaryRecordId: button.dataset.salaryRecordId})
                        });
                        const result = await response.json();
                        if (!response.ok) {
                            const unauthorized = response.status === 401;
                            showConfirmModal({
                                iconClass: "danger",
                                iconHtml: '<i class="fa-solid fa-exclamation"></i>',
                                title: unauthorized ? "로그인이 필요합니다" : "삭제에 실패했습니다",
                                message: result.message || "연봉정보 삭제에 실패했습니다.",
                                leftVisible: false,
                                rightText: unauthorized ? "로그인" : "확인",
                                rightClass: "btn-primary",
                                onRight: unauthorized ? () => location.href = "/auth/login" : null
                            });
                            return;
                        }
                        showConfirmModal({
                            iconClass: "success",
                            iconHtml: '<i class="fa-solid fa-check"></i>',
                            title: "삭제되었습니다",
                            message: result.message,
                            leftVisible: false,
                            rightText: "확인",
                            rightClass: "btn-primary",
                            onLeft: () => location.reload(),
                            onRight: () => location.reload()
                        });
                    } catch (error) {
                        showConfirmModal({
                            iconClass: "danger",
                            iconHtml: '<i class="fa-solid fa-exclamation"></i>',
                            title: "삭제에 실패했습니다",
                            message: "연봉정보 삭제 중 오류가 발생했습니다.",
                            leftVisible: false,
                            rightText: "확인",
                            rightClass: "btn-primary"
                        });
                    } finally {
                        button.disabled = false;
                    }
                }
            });
        });
    });
});
