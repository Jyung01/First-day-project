package kr.co.firstdayproject.dto.corp.applicant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record CorpApplicantDetailView(
        Long applicationId,
        String applicantName,
        String applicantInitial,
        String email,
        String phone,
        String jobTitle,
        LocalDateTime appliedAt,
        String currentStatus,
        String statusVariant,
        List<String> allowedNextStatuses,
        Map<String, Object> resume,
        Map<String, Object> coverLetter,
        List<CorpApplicationStatusHistoryItem> statusHistory,
        String managerMemo,
        String memoAuthorName,
        LocalDateTime memoUpdatedAt
) {

    public boolean hasCoverLetter() {
        return !coverLetter.isEmpty();
    }

    public boolean hasManagerMemo() {
        return managerMemo != null && !managerMemo.isBlank();
    }

    public boolean canChangeStatus() {
        return !allowedNextStatuses.isEmpty();
    }
}
