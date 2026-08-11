function initMegaMenu() {
  const megaWrap = document.getElementById('mega-wrap');
  const megaButton = document.getElementById('mega-btn');

  if (!megaWrap || !megaButton || megaWrap.dataset.initialized === 'true') {
    return;
  }

  megaWrap.dataset.initialized = 'true';

  const setOpen = (open) => {
    megaWrap.classList.toggle('open', open);
    megaButton.setAttribute('aria-expanded', String(open));
    megaButton.setAttribute('aria-label', open ? '전체 메뉴 닫기' : '전체 메뉴 열기');
  };

  megaButton.addEventListener('click', (event) => {
    event.preventDefault();
    event.stopPropagation();
    setOpen(!megaWrap.classList.contains('open'));
  });

  document.addEventListener('click', (event) => {
    if (!megaWrap.contains(event.target)) {
      setOpen(false);
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && megaWrap.classList.contains('open')) {
      setOpen(false);
      megaButton.focus();
    }
  });
}

document.addEventListener('DOMContentLoaded', initMegaMenu);
document.addEventListener('includes:loaded', initMegaMenu);
