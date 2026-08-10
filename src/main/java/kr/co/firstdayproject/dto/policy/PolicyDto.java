package kr.co.firstdayproject.dto.policy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PolicyDto {
    private Long policyId;
    private String policyCode;
    private String title;
    private String audience;
    private String consentType;
    private String content;
    private String effectiveFrom; // yyyy.MM.dd, null 가능
    private String updatedAt;     // yyyy.MM.dd
}