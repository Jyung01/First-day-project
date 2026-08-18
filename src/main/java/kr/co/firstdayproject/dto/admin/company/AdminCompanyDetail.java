package kr.co.firstdayproject.dto.admin.company;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.company.CompanyRejectionType;
import kr.co.firstdayproject.entity.member.User;

public record AdminCompanyDetail(
        Long companyId,
        String companyNumber,
        String companyName,
        String businessNumber,
        String representativeName,
        LocalDate establishedDate,
        String industryName,
        String companySize,
        String address,
        String homepageUrl,
        String shortDescription,
        String introduction,
        String benefits,
        String managerName,
        String managerEmail,
        String managerPhone,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        LocalDateTime withdrawnAt,
        String reviewTypeCode,
        String reviewTypeLabel,
        String statusCode,
        String statusLabel,
        String rejectionCode,
        String rejectionLabel,
        String rejectionReason
) {

    public static AdminCompanyDetail from(Company company, User manager) {
        CompanyRejectionType rejectionType = CompanyRejectionType
                .fromCode(company.getLatestRejectionCode())
                .orElse(null);

        return new AdminCompanyDetail(
                company.getCompanyId(),
                company.getCompanyId() == null
                        ? "-"
                        : String.format("C-%04d", company.getCompanyId()),
                display(company.getCompanyName()),
                AdminCompanyListItem.formatBusinessNumber(company.getBusinessNumber()),
                display(company.getRepresentativeName()),
                company.getEstablishedDate(),
                display(company.getIndustryName()),
                display(company.getCompanySize()),
                address(company),
                display(company.getHomepageUrl()),
                display(company.getShortDescription()),
                display(company.getIntroduction()),
                benefits(company.getBenefits()),
                manager == null ? "미등록" : display(manager.getName()),
                manager == null ? "미등록" : display(manager.getEmail()),
                manager == null ? "미등록" : display(manager.getPhone()),
                company.getReapplyRequestedAt() == null
                        ? company.getCreatedAt()
                        : company.getReapplyRequestedAt(),
                company.getReviewedAt(),
                company.getWithdrawnAt(),
                AdminCompanyListItem.reviewTypeCode(company),
                AdminCompanyListItem.reviewTypeLabel(company),
                AdminCompanyListItem.statusCode(company),
                AdminCompanyListItem.statusLabel(company),
                company.getLatestRejectionCode(),
                rejectionType == null ? null : rejectionType.getLabel(),
                company.getLatestRejectionReason()
        );
    }

    private static String address(Company company) {
        String value = Stream.of(
                        company.getPostalCode(),
                        company.getAddressLine1(),
                        company.getAddressLine2()
                )
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return display(value);
    }

    private static String benefits(String benefitsJson) {
        if (benefitsJson == null || benefitsJson.isBlank()) {
            return "미등록";
        }

        String content = benefitsJson.trim();
        if (content.length() < 2 || !content.startsWith("[") || !content.endsWith("]")) {
            return display(content);
        }

        List<String> values = Arrays.stream(
                        content.substring(1, content.length() - 1).split(",")
                )
                .map(String::trim)
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .filter(value -> !value.isBlank())
                .toList();
        return values.isEmpty() ? "미등록" : String.join(" · ", values);
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "미등록" : value.trim();
    }
}
