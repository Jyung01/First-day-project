package kr.co.firstdayproject.dto.admin.company;

public record AdminCompanyStatistics(
        long pendingCount,
        long newReviewCount,
        long reReviewCount,
        /** 가입 후 기업정보를 작성 중이라 아직 심사를 요청하지 않은 기업 수. */
        long draftCount,
        long approvedCount,
        long rejectedCount
) {
}
