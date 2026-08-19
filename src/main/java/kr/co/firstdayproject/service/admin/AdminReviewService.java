package kr.co.firstdayproject.service.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import kr.co.firstdayproject.dao.admin.AdminReviewDao;
import kr.co.firstdayproject.dto.admin.AdminReviewDTO;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewService {
    private static final int PAGE_SIZE = 10;
    private static final Set<String> TABS = Set.of("company", "interview", "hidden");
    private static final Set<String> SORTS = Set.of("latest", "oldest", "reports");
    private final AdminReviewDao adminReviewDao;

    public Map<String, Object> getList(int requestedPage, String tab, String keyword, String sort) {
        tab = TABS.contains(tab) ? tab : "company";
        sort = SORTS.contains(sort) ? sort : "latest";
        keyword = keyword == null ? "" : keyword.trim();
        Map<String, Object> search = new HashMap<>();
        search.put("tab", tab); search.put("keyword", keyword); search.put("sort", sort);
        int total = adminReviewDao.selectAdminReviewCount(search);
        int lastPage = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        int page = Math.min(Math.max(requestedPage, 1), lastPage);
        PageHandler pageHandler = new PageHandler(page, total, PAGE_SIZE);
        search.put("offset", pageHandler.getOffset()); search.put("pageSize", PAGE_SIZE);
        Map<String, Object> result = new HashMap<>();
        result.put("reviews", adminReviewDao.selectAdminReviewList(search));
        result.put("summary", adminReviewDao.selectAdminReviewSummary());
        result.put("pageHandler", pageHandler); result.put("tab", tab);
        result.put("keyword", keyword); result.put("sort", sort);
        return result;
    }

    public AdminReviewDTO getDetail(String reviewType, Long reviewId) {
        if (reviewId == null) throw new IllegalArgumentException("후기 번호가 필요합니다.");
        AdminReviewDTO detail = "기업리뷰".equals(reviewType)
                ? adminReviewDao.selectCompanyReviewDetail(reviewId)
                : "면접후기".equals(reviewType) ? adminReviewDao.selectInterviewReviewDetail(reviewId) : null;
        if (detail == null) throw new IllegalArgumentException("후기를 찾을 수 없습니다.");
        return detail;
    }

    @Transactional
    public void updateStatus(Long adminId, String reviewType, Long reviewId,
                             String status, String hiddenReason, String memo) {
        if (!Set.of("정상", "숨김").contains(status))
            throw new IllegalArgumentException("올바른 처리 상태를 선택해주세요.");
        hiddenReason = hiddenReason == null ? "" : hiddenReason.trim();
        memo = memo == null ? "" : memo.trim();
        if ("숨김".equals(status) && hiddenReason.isBlank())
            throw new IllegalArgumentException("숨김 사유를 입력해주세요.");
        if (hiddenReason.length() > 1000) throw new IllegalArgumentException("숨김 사유는 1000자 이내로 입력해주세요.");
        if (memo.length() > 2000) throw new IllegalArgumentException("관리자 메모는 2000자 이내로 입력해주세요.");
        getDetail(reviewType, reviewId);
        int changed = "기업리뷰".equals(reviewType)
                ? adminReviewDao.updateCompanyReviewStatus(reviewId, status, hiddenReason, adminId, memo)
                : adminReviewDao.updateInterviewReviewStatus(reviewId, status, hiddenReason, adminId, memo);
        if (changed != 1) throw new IllegalStateException("후기 상태를 변경하지 못했습니다.");
        if ("숨김".equals(status)) adminReviewDao.resolveReviewReports(reviewType, reviewId, adminId,
                memo.isBlank() ? hiddenReason : memo);
    }
}
