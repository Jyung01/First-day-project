package kr.co.firstdayproject.dto.job;

import java.time.LocalDateTime;

public record JobApplicationConfirmView(
        Long jobPostingId,
        String companyName,
        String companyLogoUrl,
        String jobTitle,
        String deadline,
        Long resumeId,
        String resumeTitle,
        LocalDateTime resumeUpdatedAt,
        Long coverLetterId,
        String coverLetterTitle,
        LocalDateTime coverLetterUpdatedAt
) {
}
