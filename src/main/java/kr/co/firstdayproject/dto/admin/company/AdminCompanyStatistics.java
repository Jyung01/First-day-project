package kr.co.firstdayproject.dto.admin.company;

public record AdminCompanyStatistics(
        long pendingCount,
        long newReviewCount,
        long reReviewCount,
        long approvedCount,
        long rejectedCount
) {
}
