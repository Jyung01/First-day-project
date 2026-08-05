package kr.co.firstdayproject.entity.company;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompanyRejectionTypeTest {

    @Test
    void mapsMissingInformationToRequiredCompanyFields() {
        CompanyRejectionType type = CompanyRejectionType
                .fromCode("MISSING_INFORMATION")
                .orElseThrow();

        assertThat(type.getRejectedFieldCodes()).containsExactlyInAnyOrder(
                "COMPANY_NAME",
                "BUSINESS_NUMBER",
                "REPRESENTATIVE_NAME",
                "ESTABLISHED_DATE"
        );
        assertThat(type.getRejectedFieldSummary())
                .isEqualTo("기업명 · 사업자등록번호 · 대표자명 · 설립일");
    }

    @Test
    void ignoresUnknownOrMissingCode() {
        assertThat(CompanyRejectionType.fromCode("UNKNOWN")).isEmpty();
        assertThat(CompanyRejectionType.fromCode(null)).isEmpty();
    }
}
