document.addEventListener('DOMContentLoaded', () => {

    // 로그인 후 원래 화면으로 돌아왔을 때
    // 로그인 전에 눌렀던 관심공고 자동 등록
    registerPendingBookmark();

    bindBookmarkButtons();
});

document.addEventListener('job:list-updated', bindBookmarkButtons);

function bindBookmarkButtons() {
    document.querySelectorAll('.bookmark:not([data-bookmark-bound])').forEach(button => {
        button.dataset.bookmarkBound = 'true';
        button.addEventListener('click', async event => {
            event.preventDefault();
            event.stopPropagation();

            const loggedIn =
                button.dataset.loggedIn === 'true';

            // 비로그인
            if (!loggedIn) {
                openLoginRequiredModal(button);
                return;
            }

            // 로그인 상태면 실제 관심공고 등록/해제
            await toggleBookmark(button);
        });
    });
}


function openLoginRequiredModal(bookmarkButton) {
    const modal = document.getElementById('confirmModal');
    const icon = document.getElementById('modalIcon');
    const title = document.getElementById('modalTitle');
    const message = document.getElementById('modalMessage');
    const extra = document.getElementById('modalExtra');
    const leftButton = document.getElementById('leftBtn');
    const rightButton = document.getElementById('rightBtn');

    if (
        !modal ||
        !icon ||
        !title ||
        !message ||
        !extra ||
        !leftButton ||
        !rightButton
    ) {
        console.error(
            '로그인 안내 모달 요소를 찾을 수 없습니다.'
        );
        return;
    }

    icon.className = 'modal-icon info';
    icon.textContent = '?';

    title.textContent = '로그인이 필요한 기능입니다';

    message.textContent =
        '관심공고를 등록하려면 개인회원 로그인이 필요합니다.\n' +
        '로그인 후 다시 이용해주세요.';

    extra.innerHTML = '';

    leftButton.textContent = '취소';
    rightButton.textContent = '로그인';

    leftButton.className = 'btn btn-outline';
    rightButton.className = 'btn btn-primary';

    // 취소
    leftButton.onclick = () => {
        closeLoginRequiredModal();
    };

    // 로그인
    rightButton.onclick = () => {
        // 로그인 후 등록할 공고 기억
        sessionStorage.setItem(
            'pendingBookmarkJobId',
            bookmarkButton.dataset.jobPostingId
        );

        const loginUrl =
            document.body.dataset.loginUrl || '/auth/login';

        const returnUrl =
            window.location.pathname +
            window.location.search;

        window.location.href =
            `${loginUrl}?returnUrl=${encodeURIComponent(returnUrl)}`;
    };

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
}


function closeLoginRequiredModal() {
    const modal = document.getElementById('confirmModal');

    if (!modal) {
        return;
    }

    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}


function renderBookmark(button, bookmarked) {
    button.classList.toggle(
        'active',
        bookmarked
    );

    button.textContent =
        bookmarked ? '♥' : '♡';

    button.setAttribute(
        'aria-pressed',
        String(bookmarked)
    );
}


/**
 * 로그인 전에 관심공고를 누른 경우
 * 로그인 성공 후 원래 화면으로 돌아오면 자동 등록
 */
async function registerPendingBookmark() {
    const jobPostingId =
        sessionStorage.getItem('pendingBookmarkJobId');

    if (!jobPostingId) {
        return;
    }

    const button = document.querySelector(
        `.bookmark[data-job-posting-id="${jobPostingId}"]`
    );

    // 현재 화면에 해당 공고가 없으면 종료
    if (!button) {
        sessionStorage.removeItem('pendingBookmarkJobId');
        return;
    }

    // 실제 로그인 성공 여부 확인
    const loggedIn =
        button.dataset.loggedIn === 'true';

    // 로그인에 실패했거나 로그인하지 않은 상태면
    // pending 값은 남겨둠
    if (!loggedIn) {
        return;
    }

    // 로그인 성공했으므로 pending 제거
    sessionStorage.removeItem('pendingBookmarkJobId');

    // 이미 관심공고라면 다시 toggle하면 해제되므로 종료
    if (button.classList.contains('active')) {
        return;
    }

    await toggleBookmark(button);
}


/**
 * 관심공고 등록 / 해제 API
 */
async function toggleBookmark(button) {
    const jobPostingId =
        button.dataset.jobPostingId;

    try {
        const response = await fetch(
            `/api/jobs/${jobPostingId}/bookmark`,
            {
                method: 'POST'
            }
        );

        if (!response.ok) {
            console.error(
                '관심공고 처리 실패:',
                response.status
            );
            return;
        }

        const result = await response.json();

        renderBookmark(
            button,
            result.bookmarked
        );

    } catch (error) {
        console.error(
            '관심공고 요청 오류:',
            error
        );
    }
}
