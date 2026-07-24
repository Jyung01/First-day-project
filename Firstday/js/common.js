// 직군별 직무 데이터
const jobCategoryData = {
  dev: ['백엔드 개발자','프론트엔드 개발자','데이터 엔지니어','시스템·네트워크',
        '서버 개발자','React 개발자','데이터 분석가','정보보안 담당자',
        'Java 개발자','Vue.js 개발자','데이터 사이언티스트','QA 엔지니어',
        'Node.js 개발자','웹 퍼블리셔','AI·머신러닝 엔지니어','임베디드 개발자',
        'Python 개발자','모바일 앱 개발자','MLOps 엔지니어','게임 개발자',
        'PHP 개발자','iOS 개발자','BI 엔지니어','블록체인 개발자',
        '웹 개발자','Android 개발자','DevOps 엔지니어','개발 PM',
        'DBA','크로스플랫폼 개발자','클라우드 엔지니어','CTO·테크리드'],
  biz: ['서비스 기획자','전략 기획자','신사업 기획','경영지원'],
  marketing: ['퍼포먼스 마케터','콘텐츠 마케터','브랜드 마케터','광고 기획자'],
  design: ['UX/UI 디자이너','BX 디자이너','영상·모션 디자이너','제품 디자이너'],
  cs: ['CS 매니저','매장 관리자','리테일 MD'],
  sales: ['영업 담당자','B2B 세일즈','어카운트 매니저'],
  media: ['에디터','영상 PD','콘텐츠 크리에이터'],
  engineering: ['기계 엔지니어','전기·전자 엔지니어','설계 엔지니어'],
  manufacturing: ['생산관리자','품질관리자','공정 엔지니어'],
  finance: ['재무 담당자','회계 담당자','투자 심사역'],
  medical: ['임상 연구원','바이오 연구원','제약 영업'],
  edu: ['교육 콘텐츠 기획자','강사','교육 운영자'],
  law: ['사내 변호사','법무 담당자','컴플라이언스 담당자'],
};

jobCategoryData.all = Object.values(jobCategoryData).flat();

const categoryNames = {
  all: '직군 전체',
  dev: '개발',
  biz: '경영·비즈니스',
  marketing: '마케팅·광고',
  design: '디자인',
  cs: '고객서비스·리테일',
  sales: '영업',
  media: '미디어',
  engineering: '엔지니어링·설계',
  manufacturing: '제조·생산',
  finance: '금융',
  medical: '의료·바이오',
  edu: '교육',
  law: '법률'
};

function renderMegaJobs(category) {
  const header = document.querySelector('.site-header');
  const grid = document.getElementById('mega-job-grid');
  const categoryName = document.getElementById('mega-cat-name');

  if (!header || !grid || !categoryName) {
    return;
  }

  const jobs = jobCategoryData[category] || [];
  const jobListUrl = header.dataset.jobListUrl || '/job/list.html';

  categoryName.textContent = categoryNames[category] || '';

  grid.innerHTML = jobs.map(job => {
    const url = `${jobListUrl}?job=${encodeURIComponent(job)}`;
    return `<a href="${url}" class="mega-job-item">${job}</a>`;
  }).join('');
}

function initMegaMenu() {
  const megaWrap = document.getElementById('mega-wrap');
  const megaButton = document.getElementById('mega-btn');
  const sidebarItems = document.querySelectorAll('#mega-sidebar li');

  if (!megaWrap || !megaButton || sidebarItems.length === 0) {
    return;
  }

  // DOMContentLoaded와 includes:loaded 양쪽에서 실행돼도 한 번만 바인딩
  if (megaWrap.dataset.initialized === 'true') {
    return;
  }
  megaWrap.dataset.initialized = 'true';

  sidebarItems.forEach(item => {
    item.addEventListener('click', () => {
      sidebarItems.forEach(sidebarItem => sidebarItem.classList.remove('active'));
      item.classList.add('active');
      renderMegaJobs(item.dataset.cat);
    });
  });

  megaButton.addEventListener('click', event => {
    event.preventDefault();
    event.stopPropagation();
    megaWrap.classList.toggle('open');
  });

  document.addEventListener('click', event => {
    if (!megaWrap.contains(event.target)) {
      megaWrap.classList.remove('open');
    }
  });

  renderMegaJobs('dev');
}

// Thymeleaf에서는 헤더가 처음부터 DOM에 존재
document.addEventListener('DOMContentLoaded', initMegaMenu);

// 정적 HTML에서는 include.js가 헤더를 삽입한 뒤 실행
document.addEventListener('includes:loaded', initMegaMenu);
