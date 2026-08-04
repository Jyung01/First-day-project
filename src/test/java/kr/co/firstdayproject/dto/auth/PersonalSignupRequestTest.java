package kr.co.firstdayproject.dto.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PersonalSignupRequestTest {

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
    void rejectsEmailWithoutPublicDomainSuffix() {
        PersonalSignupRequest request = validRequest();
        request.setEmail("wldnd98@na");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("email"));
    }

    @Test
    void rejectsWeakOrMismatchedPassword() {
        PersonalSignupRequest request = validRequest();
        request.setPassword("password");
        request.setPasswordConfirm("different");

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidProperties)
                .contains("password", "passwordMatched");
    }

    private PersonalSignupRequest validRequest() {
        PersonalSignupRequest request = new PersonalSignupRequest();
        request.setMemberId("member01");
        request.setPassword("Password!1");
        request.setPasswordConfirm("Password!1");
        request.setMemberName("홍길동");
        request.setPhone("010-1234-5678");
        request.setEmail("user@example.com");
        return request;
    }
}
