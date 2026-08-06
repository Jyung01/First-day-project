package kr.co.firstdayproject.dto.company;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CompanyDTO {
    private Long companyId;
    private String businessNumber;
    private String companyName;
    private String representativeName;
    private LocalDate establishedDate;
    private String industryName;
    private String companySize;
    private String homepageUrl;
    private String logoUrl;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
    private String shortDescription;
    private String introduction;
    private String benefits;
    private String approvalStatus;
    private String companyStatus;
    private String latestRejectionReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime reapplyRequestedAt;
    private Long rowVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;
    private Integer employeeCount;

    // 추가 필드
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String summary;
    private Integer hasJobPosting; // 채용중인 공고가 있는지 확인
    private Integer jobPostingCount; // 채용중인 공고 갯수
    private List<String> benefitList;
}