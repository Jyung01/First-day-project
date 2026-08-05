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
public class CompanyProfileRequest {

    @NotBlank(message = "기업명을 입력해주세요.")
    @Size(max = 200, message = "기업명은 200자를 초과할 수 없습니다.")
    private String companyName;

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

    @NotBlank(message = "주소를 검색해주세요.")
    @Size(max = 20, message = "우편번호는 20자를 초과할 수 없습니다.")
    private String postcode;

    @NotBlank(message = "주소를 검색해주세요.")
    @Size(max = 255, message = "기본주소는 255자를 초과할 수 없습니다.")
    private String address;

    @Size(max = 255, message = "상세주소는 255자를 초과할 수 없습니다.")
    private String addressDetail;

    @Size(max = 300, message = "기업 한 줄 소개는 300자를 초과할 수 없습니다.")
    private String shortDescription;

    @Size(max = 1000, message = "기업 소개·복지는 1000자를 초과할 수 없습니다.")
    private String benefits;

    public static CompanyProfileRequest from(Company company) {
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName(company.getCompanyName());
        request.setRepresentativeName(company.getRepresentativeName());
        request.setEstablishedDate(company.getEstablishedDate());
        request.setCompanySize(normalizeCompanySize(company.getCompanySize()));
        request.setIndustry(normalizeIndustry(company.getIndustryName()));
        request.setHomepage(company.getHomepageUrl());
        request.setPostcode(company.getPostalCode());
        request.setAddress(company.getAddressLine1());
        request.setAddressDetail(company.getAddressLine2());
        request.setShortDescription(company.getShortDescription());
        request.setBenefits(company.getBenefits());
        return request;
    }

    private static String normalizeIndustry(String industry) {
        if (industry == null) {
            return null;
        }
        return switch (industry) {
            case "IT_SOFTWARE" -> "IT·인터넷";
            case "MANUFACTURING" -> "제조";
            case "FINANCE" -> "금융";
            case "DISTRIBUTION" -> "유통·무역";
            case "EDUCATION" -> "교육";
            case "MEDICAL" -> "의료";
            case "SERVICE" -> "서비스";
            case "ETC" -> "기타";
            default -> industry;
        };
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
