package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MyApplicationDetailView(
        Long applicationId,
        Long jobPostingId,
        String companyName,
        String companyLogoUrl,
        String companyInitial,
        String jobTitle,
        String currentStatus,
        String statusLabel,
        String statusVariant,
        LocalDateTime appliedAt,
        Map<String, Object> resume,
        Map<String, Object> coverLetter,
        List<MyApplicationHistoryItem> statusHistory
) {
    public boolean hasCoverLetter() {
        return !coverLetter.isEmpty();
    }

    public boolean canCancel() {
        return "지원완료".equals(currentStatus);
    }

    public String applicantName() {
        return text("applicantName", "지원자");
    }

    public String email() {
        return text("email", "-");
    }

    public String phone() {
        return text("phone", "-");
    }

    private String text(String key, String fallback) {
        Object value = resume.get(key);
        return value instanceof String text && !text.isBlank()
                ? text
                : fallback;
    }
}
