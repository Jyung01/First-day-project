package kr.co.firstdayproject.dto.corp.applicant;

import java.time.LocalDateTime;

public record CorpApplicantListItem(
        Long applicationId,
        String applicantName,
        String applicantInitial,
        String jobTitle,
        LocalDateTime appliedAt,
        String applicationTypeLabel,
        String applicationTypeVariant,
        String currentStatus,
        String statusVariant
) {
}
