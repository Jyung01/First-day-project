document.addEventListener("DOMContentLoaded", () => {
    const modal = document.querySelector("[data-review-detail-modal]");
    const form = document.getElementById("adminReviewForm");
    if (!modal || !form) return;
    const status = document.getElementById("detailStatus");
    const hiddenArea = document.getElementById("hiddenReasonArea");
    const toggleHiddenReason = () => hiddenArea.style.display = status.value === "숨김" ? "block" : "none";
    status.addEventListener("change", toggleHiddenReason);

    document.querySelectorAll(".open-review-detail-modal").forEach(button => button.addEventListener("click", async () => {
        try {
            const query = new URLSearchParams({reviewType:button.dataset.reviewType, reviewId:button.dataset.reviewId});
            const response = await fetch(`/admin/review/detail?${query}`);
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "후기를 불러오지 못했습니다.");
            document.getElementById("detailReviewType").value=data.reviewType;
            document.getElementById("detailReviewId").value=data.reviewId;
            document.getElementById("detailCode").textContent=`${data.reviewType} ${data.reviewType==='기업리뷰'?'CR':'IR'}-${data.reviewId} · ${data.companyName} · 작성자 ${data.authorName}`;
            document.getElementById("detailTitle").textContent=data.title || '-';
            document.getElementById("detailContentLabel").textContent=data.reviewType==='기업리뷰'?'장점':'면접 내용';
            document.getElementById("detailSecondaryLabel").textContent=data.reviewType==='기업리뷰'?'단점':'면접 팁';
            document.getElementById("detailContent").textContent=data.content || '-';
            document.getElementById("detailSecondary").textContent=data.secondaryContent || '-';
            document.getElementById("detailDate").textContent=formatReviewDate(data.createdAt);
            document.getElementById("detailRating").textContent=data.reviewType==='기업리뷰'
                ? `평점 ${data.rating} / 5.0 · 누적 신고 ${data.reportCount}건`
                : `${data.interviewType} · ${data.interviewResult} · 난이도 ${data.difficulty} · 누적 신고 ${data.reportCount}건`;
            status.value=data.status==='숨김'?'숨김':'정상';
            document.getElementById("detailHiddenReason").value=data.hiddenReason || '';
            document.getElementById("reviewMemo").value=''; toggleHiddenReason();
            modal.classList.add("is-open"); modal.setAttribute("aria-hidden","false");
        } catch (error) { showAdminReviewMessage(false,error.message); }
    }));

    modal.querySelectorAll("[data-review-detail-close]").forEach(button => button.addEventListener("click",()=>closeAdminReviewModal(modal)));
    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (status.value==='숨김' && !document.getElementById("detailHiddenReason").value.trim()) {
            showAdminReviewMessage(false,"숨김 사유를 입력해주세요."); return;
        }
        try {
            const response=await fetch("/admin/review/status",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams(new FormData(form))});
            const result=await response.json();
            if(!response.ok) throw new Error(result.message || "후기 상태를 저장하지 못했습니다.");
            closeAdminReviewModal(modal); showAdminReviewMessage(true,result.message,true);
        } catch(error) { showAdminReviewMessage(false,error.message); }
    });
});

function closeAdminReviewModal(modal){modal.classList.remove("is-open");modal.setAttribute("aria-hidden","true");}
function formatReviewDate(value){if(!value)return '-';return new Intl.DateTimeFormat('ko-KR',{year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value));}
function showAdminReviewMessage(success,message,reload=false){showConfirmModal({iconClass:success?'success':'danger',iconHtml:success?'<i class="fa-solid fa-check"></i>':'<i class="fa-solid fa-exclamation"></i>',title:success?'처리가 완료되었습니다':'처리할 수 없습니다',message,leftVisible:false,rightText:'확인',rightClass:'btn-primary',onRight:reload?()=>location.reload():null});}
