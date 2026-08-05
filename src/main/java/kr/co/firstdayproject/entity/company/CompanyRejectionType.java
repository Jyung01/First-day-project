package kr.co.firstdayproject.entity.company;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum CompanyRejectionType {

    MISSING_INFORMATION(
            "입력 누락",
            "필수 기업정보가 누락되었습니다. 강조된 항목을 모두 입력해주세요.",
            List.of(
                    new RejectedField("COMPANY_NAME", "기업명"),
                    new RejectedField("BUSINESS_NUMBER", "사업자등록번호"),
                    new RejectedField("REPRESENTATIVE_NAME", "대표자명"),
                    new RejectedField("ESTABLISHED_DATE", "설립일")
            )
    ),
    FORMAT_ERROR(
            "형식 오류",
            "사업자등록번호 형식을 다시 확인해주세요.",
            List.of(
                    new RejectedField("BUSINESS_NUMBER", "사업자등록번호")
            )
    ),
    INAPPROPRIATE_INFORMATION(
            "부적절한 기업정보",
            "서비스에 공개하기 어려운 기업정보가 포함되어 있습니다.",
            List.of(
                    new RejectedField("COMPANY_NAME", "기업명"),
                    new RejectedField("INDUSTRY", "업종"),
                    new RejectedField("SHORT_DESCRIPTION", "기업 한 줄 소개"),
                    new RejectedField("BENEFITS", "기업 소개·복지")
            )
    );

    private final String label;
    private final String defaultMessage;
    private final List<RejectedField> rejectedFields;

    CompanyRejectionType(
            String label,
            String defaultMessage,
            List<RejectedField> rejectedFields
    ) {
        this.label = label;
        this.defaultMessage = defaultMessage;
        this.rejectedFields = List.copyOf(rejectedFields);
    }

    public Set<String> getRejectedFieldCodes() {
        return rejectedFields.stream()
                .map(RejectedField::code)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getRejectedFieldSummary() {
        return rejectedFields.stream()
                .map(RejectedField::label)
                .collect(Collectors.joining(" · "));
    }

    public static Optional<CompanyRejectionType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(code.trim()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record RejectedField(String code, String label) {
    }
}
