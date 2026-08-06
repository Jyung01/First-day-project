package kr.co.firstdayproject.dto.my;

public record DashboardStats(
        long applicationCount,
        long applicationInProgressCount,
        long likedJobCount,
        long likedJobDeadlineSoonCount,
        long likedCompanyCount,
        long likedCompanyNewJobCount,
        long resumeCount,
        long coverLetterCount
) {

    public long documentCount() {
        return resumeCount + coverLetterCount;
    }
}
