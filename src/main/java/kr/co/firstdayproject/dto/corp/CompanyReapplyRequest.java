package kr.co.firstdayproject.dto.corp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import kr.co.firstdayproject.entity.company.Company;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyReapplyRequest {

    @NotBlank(message = "기업명을 입력해주세요.")
    @Size(max = 200, message = "기업명은 200자를 초과할 수 없습니다.")
    private String companyName;

    @NotBlank(message = "사업자등록번호를 입력해주세요.")
    @Pattern(
            regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
            message = "사업자등록번호 10자리를 정확히 입력해주세요."
    )
    private String businessNumber;

    @NotBlank(message = "대표자명을 입력해주세요.")
    @Size(max = 100, message = "대표자명은 100자를 초과할 수 없습니다.")
    private String representativeName;

    private LocalDate establishedDate;

    @Pattern(
            regexp = "^$|^(스타트업|중소기업|중견기업|대기업|공공기관|외국계기업)$",
            message = "올바른 기업 규모를 선택해주세요."
    )
    private String companySize;

    @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다.")
    private String industry;

    @Size(max = 1000, message = "홈페이지 주소는 1000자를 초과할 수 없습니다.")
    private String homepage;

    @Size(max = 300, message = "기업 한 줄 소개는 300자를 초과할 수 없습니다.")
    private String shortDescription;

    @Size(max = 1000, message = "기업 소개·복지는 1000자를 초과할 수 없습니다.")
    private String benefits;

    public static CompanyReapplyRequest from(Company company) {
        CompanyReapplyRequest request = new CompanyReapplyRequest();
        request.setCompanyName(company.getCompanyName());
        request.setBusinessNumber(formatBusinessNumber(company.getBusinessNumber()));
        request.setRepresentativeName(company.getRepresentativeName());
        request.setEstablishedDate(company.getEstablishedDate());
        request.setCompanySize(normalizeCompanySize(company.getCompanySize()));
        request.setIndustry(company.getIndustryName());
        request.setHomepage(company.getHomepageUrl());
        request.setShortDescription(company.getShortDescription());
        request.setBenefits(company.getBenefits());
        return request;
    }

    private static String formatBusinessNumber(String businessNumber) {
        if (businessNumber == null) {
            return null;
        }
        String digits = businessNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return businessNumber;
        }
        return digits.substring(0, 3)
                + "-" + digits.substring(3, 5)
                + "-" + digits.substring(5);
    }

    private static String normalizeCompanySize(String companySize) {
        if (companySize == null) {
            return null;
        }
        return switch (companySize) {
            case "STARTUP" -> "스타트업";
            case "SMALL" -> "중소기업";
            case "MEDIUM" -> "중견기업";
            case "LARGE" -> "대기업";
            case "PUBLIC" -> "공공기관";
            case "FOREIGN" -> "외국계기업";
            default -> companySize;
        };
    }
}
