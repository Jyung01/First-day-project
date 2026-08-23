document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("[data-banner-modal]");
    const form = document.getElementById("bannerForm");
    const fileInput = document.getElementById("bannerFile");
    const fileName = document.getElementById("fileName");
    const title = document.getElementById("bannerModalTitle");
    const submit = document.getElementById("bannerSubmitBtn");
    let bannerId = null;
    const close = () => {
        modal.classList.remove("is-open");
        modal.setAttribute("aria-hidden", "true");
    };
    const open = () => {
        modal.classList.add("is-open");
        modal.setAttribute("aria-hidden", "false");
    };
    const setValue = (name, value) => {
        form.elements[name].value = value ?? "";
    };

    document.querySelectorAll(".review-tab .tab-btn").forEach((tab) => tab.addEventListener("click", () => {
        document.querySelectorAll(".review-tab .tab-btn").forEach((item) => item.classList.remove("active"));
        tab.classList.add("active");
        document.querySelectorAll(".banner-card").forEach((card) => {
            card.style.display = tab.dataset.tab === "all" || card.dataset.type === tab.dataset.tab ? "flex" : "none";
        });
    }));

    document.getElementById("openBannerModal")?.addEventListener("click", () => {
        bannerId = null;
        form.reset();
        setValue("displayOrder", 1);
        fileInput.required = true;
        fileName.textContent = "선택된 파일 없음";
        title.textContent = "배너 등록";
        submit.textContent = "등록";
        open();
    });
    modal.querySelectorAll("[data-banner-close]").forEach((button) => button.addEventListener("click", close));
    fileInput.addEventListener("change", () => {
        fileName.textContent = fileInput.files[0]?.name || "선택된 파일 없음";
    });

    document.querySelectorAll(".edit-banner-btn").forEach((button) => button.addEventListener("click", async (event) => {
        event.preventDefault();
        try {
            const response = await fetch(`/admin/banner/${button.dataset.bannerId}`);
            const banner = await response.json();
            if (!response.ok) throw new Error(banner.message || "배너 정보를 불러오지 못했습니다.");
            bannerId = banner.bannerId;
            form.reset();
            setValue("bannerName", banner.bannerName);
            setValue("placement", banner.placement);
            setValue("displayOrder", banner.displayOrder);
            setValue("linkUrl", banner.linkUrl);
            setValue("altText", banner.altText);
            setValue("startsAt", banner.startsAt);
            setValue("endsAt", banner.endsAt);
            fileInput.required = false;
            fileName.textContent = "기존 이미지 유지 (변경 시 파일 선택)";
            title.textContent = "배너 수정";
            submit.textContent = "수정";
            open();
        } catch (error) {
            showBannerResult(false, error.message);
        }
    }));

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        try {
            const response = await fetch(bannerId ? `/admin/banner/${bannerId}` : "/admin/banner/register", {
                method: "POST",
                body: new FormData(form)
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.message || "배너를 저장하지 못했습니다.");
            close();
            showBannerResult(true, result.message, true);
        } catch (error) {
            showBannerResult(false, error.message);
        }
    });

    document.querySelectorAll(".hide-banner-btn").forEach((button) => button.addEventListener("click", (event) => {
        event.preventDefault();
        const activate = button.dataset.active !== "true";
        showConfirmModal({
            iconClass: "warning",
            title: activate ? "배너를 노출할까요?" : "배너를 숨길까요?",
            message: activate ? "설정된 노출 기간에 사용자 화면에 표시됩니다." : "숨김 처리하면 사용자 화면에서 즉시 제외됩니다.",
            leftText: "취소",
            rightText: activate ? "노출" : "숨김",
            leftClass: "btn-outline",
            rightClass: activate ? "btn-primary" : "btn-danger",
            onRight: async () => {
                try {
                    const response = await fetch(`/admin/banner/${button.dataset.bannerId}/toggle`, {method: "POST"});
                    const result = await response.json();
                    if (!response.ok) throw new Error(result.message || "노출 상태를 변경하지 못했습니다.");
                    location.reload();
                } catch (error) {
                    showBannerResult(false, error.message);
                }
            }
        });
    }));

    document.querySelectorAll(".delete-banner-btn").forEach((button) => button.addEventListener("click", (event) => {
        event.preventDefault();
        const bannerName = button.dataset.bannerName || "선택한 배너";

        showConfirmModal({
            iconClass: "danger",
            title: "배너를 삭제할까요?",
            message: `‘${bannerName}’ 배너가 사용자 화면과 관리 목록에서 삭제됩니다. 삭제한 배너는 복구할 수 없습니다.`,
            leftText: "취소",
            rightText: "삭제",
            leftClass: "btn-outline",
            rightClass: "btn-danger",
            onRight: async () => {
                try {
                    const response = await fetch(`/admin/banner/${button.dataset.bannerId}`, {
                        method: "DELETE"
                    });
                    const result = await response.json();
                    if (!response.ok) {
                        throw new Error(result.message || "배너를 삭제하지 못했습니다.");
                    }
                    showBannerResult(true, result.message, true);
                } catch (error) {
                    showBannerResult(false, error.message);
                }
            }
        });
    }));
});

function showBannerResult(success, message, reload = false) {
    showConfirmModal({
        iconClass: success ? "success" : "danger",
        title: success ? "저장되었습니다" : "처리할 수 없습니다",
        message,
        leftVisible: false,
        rightText: "확인",
        rightClass: "btn-primary",
        onRight: reload ? () => location.reload() : null
    });
}
