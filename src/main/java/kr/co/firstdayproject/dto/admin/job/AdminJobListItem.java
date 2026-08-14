package kr.co.firstdayproject.dto.admin.job;

import java.time.LocalDateTime;

public record AdminJobListItem(
    Long jobPostingId,
    String companyName,
    String companyStatus,
    String title,
    String categoryName,
    LocalDateTime createdAt,
    LocalDateTime applyEndAt,
    String status
) {
    public boolean suspendedCompany() {
        return "이용정지".equals(companyStatus);
    }
}
