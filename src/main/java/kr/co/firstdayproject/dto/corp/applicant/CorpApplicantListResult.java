package kr.co.firstdayproject.dto.corp.applicant;

import java.util.List;
import kr.co.firstdayproject.util.PageHandler;

public record CorpApplicantListResult(
        List<CorpApplicantListItem> applicants,
        List<CorpApplicantJobOption> jobOptions,
        CorpApplicantSummary summary,
        Long jobPostingId,
        String status,
        String keyword,
        PageHandler pageHandler
) {
}
