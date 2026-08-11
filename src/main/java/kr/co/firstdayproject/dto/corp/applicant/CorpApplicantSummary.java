package kr.co.firstdayproject.dto.corp.applicant;

public record CorpApplicantSummary(
        long total,
        long waiting,
        long today
) {
}
