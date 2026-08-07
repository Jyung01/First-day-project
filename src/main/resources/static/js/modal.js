/**
 * HTML 컴포넌트 로드
 */
async function loadComponent(targetId, path) {
  try {
    const response = await fetch(path);

    if (!response.ok) {
      throw new Error(`파일을 불러올 수 없습니다. (${response.status})`);
    }

    const html = await response.text();

    document.getElementById(targetId).insertAdjacentHTML("beforeend", html);
  } catch (error) {
    console.error(error);
  }
}

/**
 * 모달 열기
 */
function openModal(modalId) {
  document.getElementById(modalId)?.classList.add("show");
}

/**
 * 모달 닫기
 */
function closeModal(modalId) {
  document.getElementById(modalId)?.classList.remove("show");
}

/**
 * ESC로 닫기
 */
document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;

  document.querySelectorAll(".modal-overlay.show").forEach((modal) => {
    modal.classList.remove("show");
  });
});

/**
 * 오버레이 클릭 시 닫기
 */
function bindOverlayClose(modalId, callback = null) {
  const modal = document.getElementById(modalId);

  modal.onclick = (e) => {
    if (e.target === modal) {
      closeModal(modalId);

      if (callback) callback();
    }
  };
}

/* ===========================================================
   Confirm Modal
=========================================================== */

function showConfirmModal({
  iconClass = "warning",
  iconHtml = '<i class="fa-solid fa-exclamation"></i>',

  title = "",
  message = "",
  extraHtml = "",

  leftText = "취소",
  rightText = "확인",

  leftClass = "btn-outline",
  rightClass = "btn-primary",

  leftVisible = true,
  rightVisible = true,

  closeOnOverlay = true,

  onLeft = null,
  onRight = null,
}) {
  const modal = document.getElementById("confirmModal");

  modal.querySelector("#modalIcon").className = `modal-icon ${iconClass}`;
  modal.querySelector("#modalIcon").innerHTML = iconHtml;

  modal.querySelector("#modalTitle").textContent = title;
  modal.querySelector("#modalMessage").textContent = message;
  modal.querySelector("#modalExtra").innerHTML = extraHtml;

  const leftBtn = modal.querySelector("#leftBtn");
  const rightBtn = modal.querySelector("#rightBtn");

  const footer = modal.querySelector(".modal-footer");

  leftBtn.style.display = leftVisible ? "block" : "none";
  rightBtn.style.display = rightVisible ? "block" : "none";

  if (leftVisible && rightVisible) {
    footer.classList.remove("one-button");
  } else {
    footer.classList.add("one-button");
  }

  leftBtn.textContent = leftText;
  rightBtn.textContent = rightText;

  leftBtn.className = `btn ${leftClass}`;
  rightBtn.className = `btn ${rightClass}`;

  leftBtn.onclick = () => {
    closeModal("confirmModal");
    onLeft?.();
  };

  rightBtn.onclick = () => {
    closeModal("confirmModal");
    onRight?.();
  };

  if (closeOnOverlay) {
    bindOverlayClose("confirmModal", onLeft);
  } else {
    modal.onclick = null;
  }

  openModal("confirmModal");
}

/* ===========================================================
   Form Modal
=========================================================== */

function showFormModal({
  title = "",
  bodyHtml = "",

  leftText = "취소",
  rightText = "저장",
  leftClass = "btn-outline",
  rightClass = "btn-primary",

  closeOnOverlay = true,

  onLeft = null,
  onRight = null,
}) {
  const modal = document.getElementById("formModal");

  modal.querySelector("#formTitle").textContent = title;
  modal.querySelector("#formBody").innerHTML = bodyHtml;

  const leftBtn = modal.querySelector("#formLeftBtn");
  const rightBtn = modal.querySelector("#formRightBtn");

  leftBtn.textContent = leftText;
  rightBtn.textContent = rightText;
  leftBtn.className = `btn ${leftClass}`;
  rightBtn.className = `btn ${rightClass}`;

  leftBtn.onclick = () => {
    closeModal("formModal");
    onLeft?.();
  };

  rightBtn.onclick = () => {
    onRight?.();
  };

  if (closeOnOverlay) {
    bindOverlayClose("formModal", onLeft);
  } else {
    modal.onclick = null;
  }

  openModal("formModal");
}

/* ===========================================================
   Select Modal
=========================================================== */

function showSelectModal({
  title = "",
  bodyHtml = "",

  leftText = "취소",
  rightText = "선택 완료",

  onLeft = null,
  onRight = null,
}) {
  const modal = document.getElementById("selectModal");

  modal.querySelector("#selectTitle").textContent = title;
  modal.querySelector("#selectBody").innerHTML = bodyHtml;

  const leftBtn = modal.querySelector("#selectCancelBtn");
  const rightBtn = modal.querySelector("#selectConfirmBtn");

  leftBtn.textContent = leftText;
  rightBtn.textContent = rightText;

  leftBtn.onclick = () => {
    closeModal("selectModal");
    onLeft?.();
  };

  rightBtn.onclick = () => {
    onRight?.();
  };

  bindOverlayClose("selectModal", onLeft);

  openModal("selectModal");
}
