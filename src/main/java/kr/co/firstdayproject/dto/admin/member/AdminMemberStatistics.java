package kr.co.firstdayproject.dto.admin.member;

public record AdminMemberStatistics(
        long totalCount,
        long todayCount,
        long suspendedCount
) {
}
