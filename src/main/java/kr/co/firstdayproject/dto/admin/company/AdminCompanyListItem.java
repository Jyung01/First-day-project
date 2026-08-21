package kr.co.firstdayproject.dto.admin.company;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;

public record AdminCompanyListItem(
        Long companyId,
        String companyNumber,
        String companyName,
        String businessNumber,
        String managerName,
        LocalDateTime requestedAt,
        String reviewTypeCode,
        String reviewTypeLabel,
        String statusCode,
        String statusLabel
) {

    public static AdminCompanyListItem from(Company company, User manager) {
        return new AdminCompanyListItem(
                company.getCompanyId(),
                formatCompanyNumber(company.getCompanyId()),
                company.getCompanyName(),
                formatBusinessNumber(company.getBusinessNumber()),
                manager == null ? "미등록" : manager.getName(),
                requestedAt(company),
                reviewTypeCode(company),
                reviewTypeLabel(company),
                statusCode(company),
                statusLabel(company)
        );
    }

    public static String statusCode(Company company) {
        if ("탈퇴".equals(company.getCompanyStatus())) {
            return "WITHDRAWN";
        }
        if ("이용정지".equals(company.getCompanyStatus())) {
            return "SUSPENDED";
        }
        return switch (company.getApprovalStatus()) {
            case "승인" -> "APPROVED";
            case "반려" -> "REJECTED";
            /*
             * 같은 '승인대기'라도 아직 심사를 요청하지 않았으면 심사할 수 없다.
             * 심사 큐(PENDING 탭)에는 안 나오지만 '전체' 탭에는 보이므로,
             * 관리자가 심사 가능한 건과 구분할 수 있도록 상태를 나눠준다.
             */
            default -> company.getReviewRequestedAt() == null ? "DRAFT" : "PENDING";
        };
    }

    public static String statusLabel(Company company) {
        return switch (statusCode(company)) {
            case "APPROVED" -> "승인";
            case "REJECTED" -> "반려";
            case "SUSPENDED" -> "이용정지";
            case "WITHDRAWN" -> "탈퇴";
            case "DRAFT" -> "작성 중";
            default -> "승인 대기";
        };
    }

    public static String reviewTypeCode(Company company) {
        return company.getReapplyRequestedAt() == null ? "NEW" : "REVIEW";
    }

    public static String reviewTypeLabel(Company company) {
        if (!"PENDING".equals(statusCode(company))) {
            return "-";
        }
        return "REVIEW".equals(reviewTypeCode(company)) ? "재심사" : "신규 심사";
    }

    private static LocalDateTime requestedAt(Company company) {
        return company.getReapplyRequestedAt() == null
                ? company.getCreatedAt()
                : company.getReapplyRequestedAt();
    }

    private static String formatCompanyNumber(Long companyId) {
        return companyId == null ? "-" : String.format("C-%04d", companyId);
    }

    static String formatBusinessNumber(String businessNumber) {
        if (businessNumber == null || businessNumber.isBlank()) {
            return "-";
        }

        String digits = businessNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return businessNumber.trim();
        }
        return digits.substring(0, 3)
                + "-"
                + digits.substring(3, 5)
                + "-"
                + digits.substring(5);
    }
}
