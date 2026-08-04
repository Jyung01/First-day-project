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
import kr.co.firstdayproject.dto.auth.PersonalSignupRequest;
import kr.co.firstdayproject.dto.auth.PersonalTermsAgreement;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.member.PersonalProfileRepository;
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
class PersonalSignupServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PersonalProfileRepository personalProfileRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private UserDesiredJobRepository userDesiredJobRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private UserPolicyConsentRepository userPolicyConsentRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PersonalSignupService personalSignupService;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties(
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                Duration.ofMinutes(30),
                5
        );
        personalSignupService = new PersonalSignupService(
                userRepository,
                personalProfileRepository,
                jobCategoryRepository,
                userDesiredJobRepository,
                policyRepository,
                userPolicyConsentRepository,
                passwordEncoder,
                properties
        );
    }

    @Test
    void savesPersonalAccountProfileJobsAndPolicyConsents() {
        PersonalSignupRequest request = validRequest();
        LocalDateTime now = LocalDateTime.now();
        PersonalTermsAgreement agreement = new PersonalTermsAgreement(
                Set.of(1L, 2L),
                Set.of(1L, 2L, 3L),
                now
        );
        VerifiedEmail verifiedEmail = new VerifiedEmail(
                "user@example.com",
                now
        );

        when(passwordEncoder.encode("Password!1")).thenReturn("encoded-password");
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(List.of(
                        policy(1L, "필수"),
                        policy(2L, "필수"),
                        policy(3L, "선택")
                ));
        when(jobCategoryRepository
                .findAllByJobCategoryIdInAndIsActiveTrueAndDepth(List.of(11L, 12L), 2))
                .thenReturn(List.of(job(11L), job(12L)));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setUserId(100L);
                    return user;
                });

        Long userId = personalSignupService.signup(
                request,
                agreement,
                verifiedEmail,
                "127.0.0.1",
                "test-agent"
        );

        assertThat(userId).isEqualTo(100L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash())
                .isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getUserType()).isEqualTo("개인");

        ArgumentCaptor<PersonalProfile> profileCaptor =
                ArgumentCaptor.forClass(PersonalProfile.class);
        verify(personalProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUserId()).isEqualTo(100L);
        assertThat(profileCaptor.getValue().getPostalCode()).isEqualTo("06236");

        verify(userDesiredJobRepository).saveAll(any());
        verify(userPolicyConsentRepository).saveAll(any());
    }

    @Test
    void rejectsEmailThatWasNotVerified() {
        PersonalSignupRequest request = validRequest();

        assertThatThrownBy(() -> personalSignupService.signup(
                request,
                new PersonalTermsAgreement(Set.of(1L), Set.of(1L), LocalDateTime.now()),
                new VerifiedEmail("other@example.com", LocalDateTime.now()),
                null,
                null
        ))
                .isInstanceOf(PersonalSignupException.class)
                .hasMessageContaining("이메일 인증");

        verify(userRepository, never()).saveAndFlush(any());
    }

    private PersonalSignupRequest validRequest() {
        PersonalSignupRequest request = new PersonalSignupRequest();
        request.setMemberId("member01");
        request.setPassword("Password!1");
        request.setPasswordConfirm("Password!1");
        request.setMemberName("홍길동");
        request.setPhone("010-1234-5678");
        request.setEmail("user@example.com");
        request.setDesiredJobIds(List.of(11L, 12L));
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

    private JobCategory job(Long id) {
        return JobCategory.builder()
                .jobCategoryId(id)
                .depth(2)
                .isActive(true)
                .build();
    }
}
