document.addEventListener("DOMContentLoaded", () => {

    // ===============================
    // 탭 메뉴
    // ===============================
    const tabs = document.querySelectorAll(".tab-btn");
    const banners = document.querySelectorAll(".banner-card");

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            tabs.forEach((btn) => btn.classList.remove("active"));
            tab.classList.add("active");

            const type = tab.dataset.tab;

            banners.forEach((banner) => {
                if (type === "all" || banner.dataset.type === type) {
                    banner.style.display = "flex";
                } else {
                    banner.style.display = "none";
                }
            });
        });
    });

    // ===============================
    // 배너 등록 모달
    // ===============================
    const bannerModal = document.querySelector("[data-banner-modal]");
    const openBannerBtn = document.getElementById("openBannerModal");

    const modalTitle = document.getElementById("bannerModalTitle");
    const submitBtn = document.getElementById("bannerSubmitBtn");
    const bannerForm = document.getElementById("bannerForm");

    let editMode = false;
    let bannerId = null;

    if (bannerModal && openBannerBtn) {
        openBannerBtn.addEventListener("click", () => {

            editMode = false;
            bannerId = null;

            bannerForm.reset();
            fileName.textContent = "선택된 파일 없음";

            modalTitle.textContent = "배너 등록";
            submitBtn.textContent = "저장";

            bannerModal.classList.add("is-open");
            bannerModal.setAttribute("aria-hidden", "false")
        });

        document.querySelectorAll("[data-banner-close]").forEach((btn) => {
            btn.addEventListener("click", () => {
                bannerModal.classList.remove("is-open");
                bannerModal.setAttribute("aria-hidden", "true");
            });
        });
    }

    // ===============================
    // 배너 등록 - 파일 첨부
    // ===============================
    const bannerFile = document.getElementById("bannerFile");
    const fileName = document.getElementById("fileName");

    if (bannerFile && fileName) {
        bannerFile.addEventListener("change", () => {
            fileName.textContent =
                bannerFile.files.length > 0
                    ? bannerFile.files[0].name
                    : "선택된 파일 없음";
        });
    }

    // ===============================
    // 수정하기 버튼 클릭
    // ===============================
    document.querySelectorAll(".edit-banner-btn").forEach((btn) => {

        btn.addEventListener("click", (e) => {

            e.preventDefault();

            editMode = true;
            bannerId = btn.dataset.bannerId;

            // TODO
            // const response = await fetch(`/admin/banner/${bannerId}`);
            // const banner = await response.json();

            // TODO 조회 데이터 화면에 세팅
            // document.querySelector('input[name="title"]').value = banner.title;
            // document.querySelector('select[name="position"]').value = banner.position;
            // document.querySelector('input[name="priority"]').value = banner.priority;
            // document.querySelector('input[name="url"]').value = banner.url;
            // document.querySelector('input[name="altText"]').value = banner.altText;
            // document.querySelector('input[name="startDate"]').value = banner.startDate;
            // document.querySelector('input[name="endDate"]').value = banner.endDate;

            // TODO 기존 이미지명 표시
            // document.getElementById("fileName").textContent = banner.imageName;

            modalTitle.textContent = "배너 수정";
            submitBtn.textContent = "수정";

            bannerModal.classList.add("is-open");

        });

    });

    // ===============================
    // 등록/수정 submit
    // ===============================
    bannerForm.addEventListener("submit", (e) => {

        e.preventDefault();

        if (editMode) {

            // TODO
            // PUT /admin/banner/{bannerId}

        } else {

            // TODO
            // POST /admin/banner

        }

        bannerModal.classList.remove("is-open");

    });

    // ===============================
    // 배너 숨김
    // ===============================
    document.querySelectorAll(".hide-banner-btn").forEach((btn) => {

        btn.addEventListener("click", (e) => {

            e.preventDefault();

            const bannerId = btn.dataset.bannerId;

            showConfirmModal({

                iconClass: "warning",
                iconHtml: '<i class="fa-solid fa-eye-slash"></i>',

                title: "배너를 숨길까요?",
                message: "숨김 처리하면 사용자에게 더 이상 노출되지 않습니다.",

                leftText: "취소",
                rightText: "숨김",

                leftClass: "btn-outline",
                rightClass: "btn-danger",

                onRight: async () => {

                    // TODO : 배너 숨김 API
                    //
                    // await fetch(`/admin/banner/${bannerId}/hide`, {
                    //     method: "PATCH"
                    // });
                    //
                    // 성공 시
                    // location.reload();
                    //
                    // 또는 상태만 변경
                    // const card = btn.closest(".banner-card");
                    // card.querySelector(".status").textContent = "숨김";
                    // card.querySelector(".status").classList.remove("on");
                    // card.querySelector(".status").classList.add("off");

                }

            });

        });

    });


});
