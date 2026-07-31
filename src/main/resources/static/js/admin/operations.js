document.addEventListener("DOMContentLoaded", () => {
  const categoryBody = () => `
    <div class="modal-form-body">
      <div class="modal-form-grid">
        <div class="form-group">
          <label for="categoryType">구분</label>
          <select id="categoryType">
            <option>2차 카테고리</option>
            <option>1차 카테고리</option>
          </select>
        </div>
        <div class="form-group">
          <label for="parentCategory">상위 카테고리</label>
          <select id="parentCategory">
            <option>개발·데이터</option>
          </select>
        </div>
        <div class="form-group form-group--wide">
          <label for="categoryName">카테고리명</label>
          <input
            id="categoryName"
            placeholder="카테고리명을 입력하세요."
          />
        </div>
      </div>
      <p class="modal-notice">
        회원 희망 직무·채용공고에서 사용 중인 직무는 삭제할 수 없습니다.
        미사용 항목만 삭제할 수 있습니다.
      </p>
    </div>
  `;

  const skillBody = () => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="skillCategory">기술 분류</label>
        <select id="skillCategory">
          <option>언어</option>
          <option>프레임워크</option>
          <option>데이터베이스</option>
        </select>
      </div>
      <div class="form-group">
        <label for="skillName">기술명</label>
        <input
          id="skillName"
          placeholder="기술명을 입력하세요."
        />
      </div>
      <p class="modal-notice">
        이력서·채용공고에서 사용 중인 기술은 삭제할 수 없습니다.
        미사용 항목만 삭제할 수 있습니다.
      </p>
    </div>
  `;

  const hideJobBody = () => `
    <div class="modal-form-body">
      <div class="form-group">
        <label for="hideReason">숨김 사유 *</label>
        <textarea
          id="hideReason"
          placeholder="위반 또는 부적절한 채용 정보를 입력하세요."
        ></textarea>
      </div>
      <p class="modal-notice modal-notice--danger">
        숨김 처리 후 기업회원은 수정 뒤 재검토를 요청할 수 있습니다.
        입력한 숨김 사유는 기업회원에게 표시됩니다.
      </p>
    </div>
  `;

  document.querySelectorAll("[data-modal-open]").forEach((button) => {
    button.addEventListener("click", () => {
      const modalType = button.dataset.modalOpen;
      const title = button.dataset.modalTitle;
      const value = button.dataset.modalValue || "";

      if (modalType === "category") {
        showFormModal({
          title: title || "카테고리 등록",
          bodyHtml: categoryBody(),
          onRight: () => closeModal("formModal"),
        });
        document.querySelector("#categoryName").value = value;
      }

      if (modalType === "skill") {
        showFormModal({
          title: title || "기술 스택 등록",
          bodyHtml: skillBody(),
          onRight: () => closeModal("formModal"),
        });
        document.querySelector("#skillName").value = value;
      }

      if (modalType === "job-hide") {
        showFormModal({
          title: "채용공고 숨김 처리",
          bodyHtml: hideJobBody(),
          rightText: "숨김 처리",
          rightClass: "btn-danger",
          onRight: () => closeModal("formModal"),
        });
      }
    });
  });
});
