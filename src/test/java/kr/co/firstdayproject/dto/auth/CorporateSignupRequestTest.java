package kr.co.firstdayproject.dto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CorporateSignupRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidSignupRequest() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsInvalidBusinessNumberAndMissingAddress() {
        CorporateSignupRequest request = validRequest();
        request.setBusinessNumber("123-45");
        request.setPostcode("");
        request.setAddress("");

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(invalidProperties)
                .contains("businessNumber", "postcode", "address");
    }

    @Test
    void rejectsWeakOrMismatchedPassword() {
        CorporateSignupRequest request = validRequest();
        request.setPassword("password");
        request.setPasswordConfirm("different");

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(invalidProperties)
                .contains("password", "passwordMatched");
    }

    private CorporateSignupRequest validRequest() {
        CorporateSignupRequest request = new CorporateSignupRequest();
        request.setMemberId("company01");
        request.setPassword("Password!1");
        request.setPasswordConfirm("Password!1");
        request.setManagerName("김담당");
        request.setManagerPhone("010-1234-5678");
        request.setManagerEmail("manager@example.com");
        request.setBusinessNumber("123-45-67890");
        request.setCompanyName("첫출근 주식회사");
        request.setIndustry("IT·인터넷");
        request.setCompanySize("중소기업");
        request.setPostcode("06236");
        request.setAddress("서울특별시 강남구 테헤란로 1");
        request.setAddressDetail("101호");
        return request;
    }
}
