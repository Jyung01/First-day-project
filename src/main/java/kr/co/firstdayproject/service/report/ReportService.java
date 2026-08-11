package kr.co.firstdayproject.service.report;

import java.util.Map;
import java.util.Set;
import kr.co.firstdayproject.dao.report.ReportDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final Map<String, String> TARGET_TYPES = Map.of(
            "company", "기업", "jobPosting", "채용공고",
            "companyReview", "기업리뷰", "interviewReview", "면접후기");
    private static final Set<String> REASONS = Set.of(
            "허위 정보·사기 의심", "개인정보 노출", "욕설·비방·차별 표현",
            "광고·스팸·중복 콘텐츠", "기타 운영정책 위반");
    private final ReportDao reportDao;

    @Transactional
    public void report(Long userId, String reportType, Long targetId, String reasonCode, String detail) {
        String targetType = TARGET_TYPES.get(reportType);
        if (targetType == null) throw new IllegalArgumentException("올바른 신고 대상을 선택해주세요.");
        if (targetId == null || reportDao.selectTargetCount(targetType, targetId) == 0)
            throw new IllegalArgumentException("신고 대상을 찾을 수 없습니다.");
        if (!REASONS.contains(reasonCode)) throw new IllegalArgumentException("신고 사유를 선택해주세요.");
        detail = detail == null ? "" : detail.trim();
        if (detail.length() > 2000) throw new IllegalArgumentException("상세 내용은 2000자 이내로 입력해주세요.");
        if (reportDao.selectDuplicateCount(userId, targetType, targetId) > 0)
            throw new IllegalStateException("이미 신고한 대상입니다.");
        if (reportDao.insertReport(userId, targetType, targetId, reasonCode, detail) != 1)
            throw new IllegalStateException("신고를 접수하지 못했습니다.");
    }
}
