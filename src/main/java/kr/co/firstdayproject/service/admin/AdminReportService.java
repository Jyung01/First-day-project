package kr.co.firstdayproject.service.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import kr.co.firstdayproject.dao.admin.AdminReportDao;
import kr.co.firstdayproject.dto.admin.AdminReportDTO;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private static final int PAGE_SIZE = 10;
    private static final Set<String> TABS = Set.of("all", "waiting", "complete", "reject");
    private static final Set<String> TARGET_TYPES = Set.of("기업", "채용공고", "기업리뷰", "면접후기");
    private static final Set<String> REASONS = Set.of("허위 정보·사기 의심", "개인정보 노출", "욕설·비방·차별 표현", "광고·스팸·중복 콘텐츠", "기타 운영정책 위반");
    private final AdminReportDao adminReportDao;

    public Map<String, Object> getList(int requestedPage, String tab, String targetType, String reasonCode, String keyword) {
        tab = TABS.contains(tab) ? tab : "all";
        targetType = TARGET_TYPES.contains(targetType) ? targetType : "";
        reasonCode = REASONS.contains(reasonCode) ? reasonCode : "";
        keyword = keyword == null ? "" : keyword.trim();
        Map<String, Object> search = new HashMap<>();
        search.put("tab", tab); search.put("targetType", targetType); search.put("reasonCode", reasonCode); search.put("keyword", keyword);
        int total = adminReportDao.selectReportCount(search);
        int page = Math.min(Math.max(requestedPage, 1), Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE)));
        PageHandler pageHandler = new PageHandler(page, total, PAGE_SIZE);
        search.put("offset", pageHandler.getOffset()); search.put("pageSize", PAGE_SIZE);
        Map<String, Object> result = new HashMap<>();
        result.put("reports", adminReportDao.selectReportList(search));
        result.put("summary", adminReportDao.selectReportSummary(search));
        result.put("pageHandler", pageHandler); result.put("tab", tab); result.put("targetType", targetType);
        result.put("reasonCode", reasonCode); result.put("keyword", keyword);
        return result;
    }

    public AdminReportDTO getDetail(Long reportId) {
        AdminReportDTO report = adminReportDao.selectReportDetail(reportId);
        if (report == null) throw new IllegalArgumentException("신고를 찾을 수 없습니다.");
        return report;
    }

    @Transactional
    public void process(Long adminId, Long reportId, String action, String memo) {
        if (!Set.of("REJECT", "HIDE").contains(action)) throw new IllegalArgumentException("처리 방식을 선택해주세요.");
        memo = memo == null ? "" : memo.trim();
        if (memo.length() > 2000) throw new IllegalArgumentException("관리자 메모는 2000자 이내로 입력해주세요.");
        AdminReportDTO report = getDetail(reportId);
        if (!"미처리".equals(report.getStatus())) throw new IllegalStateException("이미 처리된 신고입니다.");
        if ("REJECT".equals(action)) {
            if (adminReportDao.rejectReport(reportId, adminId, memo) != 1) throw new IllegalStateException("신고를 기각하지 못했습니다.");
            return;
        }
        int changed = switch (report.getTargetType()) {
            case "기업" -> adminReportDao.hideCompany(report.getTargetId());
            case "채용공고" -> adminReportDao.hideJobPosting(report.getTargetId(), adminId, memo);
            case "기업리뷰" -> adminReportDao.hideCompanyReview(report.getTargetId(), adminId, memo);
            case "면접후기" -> adminReportDao.hideInterviewReview(report.getTargetId(), adminId, memo);
            default -> 0;
        };
        if (changed != 1) throw new IllegalStateException("신고 대상을 숨김 처리하지 못했습니다.");
        adminReportDao.resolveReportsForTarget(report.getTargetType(), report.getTargetId(), adminId, memo);
    }
}
