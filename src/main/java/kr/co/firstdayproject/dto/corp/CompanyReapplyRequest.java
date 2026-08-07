package kr.co.firstdayproject.dto.corp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Size(max = 2000, message = "기업 소개는 2000자를 초과할 수 없습니다.")
    private String introduction;

    @Size(max = 10, message = "복리후생은 최대 10개까지 선택할 수 있습니다.")
    private List<String> benefits = new ArrayList<>();

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
        request.setIntroduction(company.getIntroduction());
        request.setBenefits(parseBenefits(company.getBenefits()));
        return request;
    }

    private static List<String> parseBenefits(String benefitsJson) {
        if (benefitsJson == null || benefitsJson.isBlank()) {
            return new ArrayList<>();
        }

        String content = benefitsJson.trim();
        if (content.length() < 2 || content.charAt(0) != '[') {
            return new ArrayList<>();
        }

        return Arrays.stream(
                content.substring(1, content.length() - 1).split(",")
            )
            .map(String::trim)
            .map(value -> value.replaceAll("^\"|\"$", ""))
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
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
