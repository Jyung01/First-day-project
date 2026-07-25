// 탭 전환 (인기 공고 / 최신 공고, AI 추천은 잠금 표시)
  document.querySelectorAll('.tab:not(.locked)').forEach(tab=>{
    tab.addEventListener('click', ()=>{
      document.querySelectorAll('.tab:not(.locked)').forEach(t=>t.classList.remove('active'));
      tab.classList.add('active');
    });
  });