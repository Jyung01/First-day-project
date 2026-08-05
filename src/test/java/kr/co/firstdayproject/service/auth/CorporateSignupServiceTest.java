package kr.co.firstdayproject.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.CorporateSignupRequest;
import kr.co.firstdayproject.dto.auth.CorporateTermsAgreement;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.policy.PolicyRepository;
import kr.co.firstdayproject.repository.policy.UserPolicyConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CorporateSignupServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private UserPolicyConsentRepository userPolicyConsentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CorporateSignupService corporateSignupService;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties(
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                Duration.ofMinutes(30),
                5
        );
        corporateSignupService = new CorporateSignupService(
                companyRepository,
                userRepository,
                policyRepository,
                userPolicyConsentRepository,
                passwordEncoder,
                properties
        );
    }

    @Test
    void acceptsAvailableBusinessNumberAndChecksBothStoredFormats() {
        List<String> businessNumbers = List.of(
                "1234567890",
                "123-45-67890"
        );
        when(companyRepository.existsByBusinessNumberIn(businessNumbers))
                .thenReturn(false);

        boolean result = corporateSignupService
                .isBusinessNumberAvailable("123-45-67890");

        assertThat(result).isTrue();
        verify(companyRepository).existsByBusinessNumberIn(businessNumbers);
    }

    @Test
    void rejectsDuplicatedBusinessNumberRegardlessOfStoredFormat() {
        List<String> businessNumbers = List.of(
                "1234567890",
                "123-45-67890"
        );
        when(companyRepository.existsByBusinessNumberIn(businessNumbers))
                .thenReturn(true);

        boolean result = corporateSignupService
                .isBusinessNumberAvailable("1234567890");

        assertThat(result).isFalse();
    }

    @Test
    void rejectsInvalidBusinessNumberWithoutRepositoryLookup() {
        assertThat(corporateSignupService.isBusinessNumberAvailable("123-45"))
                .isFalse();
        assertThat(corporateSignupService.isBusinessNumberAvailable(null))
                .isFalse();
    }

    @Test
    void savesCompanyAccountAndPolicyConsents() {
        CorporateSignupRequest request = validRequest();
        LocalDateTime now = LocalDateTime.now();
        CorporateTermsAgreement agreement = new CorporateTermsAgreement(
                Set.of(1L, 2L),
                Set.of(1L, 2L, 3L),
                now
        );
        VerifiedEmail verifiedEmail = new VerifiedEmail(
                "manager@example.com",
                now
        );

        when(passwordEncoder.encode("Password!1")).thenReturn("encoded-password");
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(List.of(
                        policy(1L, "필수"),
                        policy(2L, "필수"),
                        policy(3L, "선택")
                ));
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenAnswer(invocation -> {
                    Company company = invocation.getArgument(0);
                    company.setCompanyId(50L);
                    return company;
                });
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setUserId(100L);
                    return user;
                });

        Long userId = corporateSignupService.signup(
                request,
                agreement,
                verifiedEmail,
                "127.0.0.1",
                "test-agent"
        );

        assertThat(userId).isEqualTo(100L);

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).saveAndFlush(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getBusinessNumber()).isEqualTo("1234567890");
        assertThat(companyCaptor.getValue().getCompanySize()).isEqualTo("중소기업");
        assertThat(companyCaptor.getValue().getApprovalStatus()).isEqualTo("승인대기");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getCompanyId()).isEqualTo(50L);
        assertThat(userCaptor.getValue().getUserType()).isEqualTo("기업");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");

        verify(userPolicyConsentRepository).saveAll(any());
    }

    @Test
    void rejectsEmailThatWasNotVerified() {
        CorporateSignupRequest request = validRequest();

        assertThatThrownBy(() -> corporateSignupService.signup(
                request,
                new CorporateTermsAgreement(Set.of(1L), Set.of(1L), LocalDateTime.now()),
                new VerifiedEmail("other@example.com", LocalDateTime.now()),
                null,
                null
        ))
                .isInstanceOf(CorporateSignupException.class)
                .hasMessageContaining("이메일 인증");

        verify(companyRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).saveAndFlush(any());
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

    private Policy policy(Long id, String consentType) {
        return Policy.builder()
                .policyId(id)
                .consentType(consentType)
                .build();
    }
}
