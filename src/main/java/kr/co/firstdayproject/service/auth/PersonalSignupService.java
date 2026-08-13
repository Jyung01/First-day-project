package kr.co.firstdayproject.service.auth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.PersonalSignupRequest;
import kr.co.firstdayproject.dto.auth.PersonalTermsAgreement;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.UserDesiredJob;
import kr.co.firstdayproject.entity.job.UserDesiredJobId;
import kr.co.firstdayproject.entity.member.PersonalProfile;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.entity.policy.UserPolicyConsent;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.member.PersonalProfileRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.repository.policy.PolicyRepository;
import kr.co.firstdayproject.repository.policy.UserPolicyConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalSignupService {

    private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{6,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
    );

    private final UserRepository userRepository;
    private final PersonalProfileRepository personalProfileRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserDesiredJobRepository userDesiredJobRepository;
    private final PolicyRepository policyRepository;
    private final UserPolicyConsentRepository userPolicyConsentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationProperties emailVerificationProperties;

    public boolean isLoginIdAvailable(String loginId) {
        if (loginId == null || !LOGIN_ID_PATTERN.matcher(loginId.trim()).matches()) {
            return false;
        }
        return !userRepository.existsByLoginIdIgnoreCase(loginId.trim());
    }

    public boolean isVerifiedEmail(String email, VerifiedEmail verifiedEmail) {
        if (email == null || verifiedEmail == null) {
            return false;
        }

        LocalDateTime validAfter = LocalDateTime.now()
                .minus(emailVerificationProperties.verifiedValidity());
        return normalizeEmail(email).equals(verifiedEmail.email())
                && !verifiedEmail.verifiedAt().isBefore(validAfter);
    }

    @Transactional
    public Long signup(
            PersonalSignupRequest request,
            PersonalTermsAgreement termsAgreement,
            VerifiedEmail verifiedEmail,
            String ipAddress,
            String userAgent
    ) {
        String loginId = request.getMemberId().trim();
        String email = normalizeEmail(request.getEmail());

        validateAccount(request, loginId, email, verifiedEmail);

        LocalDateTime now = LocalDateTime.now();
        List<Policy> policies = validateTermsAgreement(termsAgreement, now);
        List<Long> desiredJobIds = validateDesiredJobs(request.getDesiredJobIds());

        User user = User.builder()
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getMemberName().trim())
                .email(email)
                .phone(request.getPhone().trim())
                .userType("개인")
                .accountStatus("정상")
                .passwordChangedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new PersonalSignupException(
                    null,
                    "이미 사용 중인 아이디 또는 이메일입니다."
            );
        }

        personalProfileRepository.save(PersonalProfile.builder()
                .userId(savedUser.getUserId())
                .postalCode(trimToNull(request.getPostcode()))
                .addressLine1(trimToNull(request.getAddress()))
                .addressLine2(trimToNull(request.getAddressDetail()))
                .createdAt(now)
                .updatedAt(now)
                .build());

        saveDesiredJobs(savedUser.getUserId(), desiredJobIds, now);
        savePolicyConsents(
                savedUser.getUserId(),
                policies,
                termsAgreement.agreedPolicyIds(),
                now,
                truncate(ipAddress, 45),
                truncate(userAgent, 500)
        );

        return savedUser.getUserId();
    }

    private void validateAccount(
            PersonalSignupRequest request,
            String loginId,
            String email,
            VerifiedEmail verifiedEmail
    ) {
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new PersonalSignupException(
                    "memberId",
                    "아이디는 영문과 숫자 6~20자로 입력해주세요."
            );
        }
        if (userRepository.existsByLoginIdIgnoreCase(loginId)) {
            throw new PersonalSignupException(
                    "memberId",
                    "이미 사용 중인 아이디입니다."
            );
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new PersonalSignupException(
                    "email",
                    "이미 가입된 이메일입니다."
            );
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new PersonalSignupException(
                    "password",
                    "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new PersonalSignupException(
                    "passwordConfirm",
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
        if (!isVerifiedEmail(email, verifiedEmail)) {
            throw new PersonalSignupException(
                    "email",
                    "이메일 인증이 만료되었거나 인증한 이메일과 일치하지 않습니다."
            );
        }
    }

    private List<Policy> validateTermsAgreement(
            PersonalTermsAgreement agreement,
            LocalDateTime now
    ) {
        if (agreement == null) {
            throw new PersonalSignupException("terms", "약관 동의 정보가 없습니다.");
        }

        List<Policy> policies = policyRepository.findActiveSignupPolicies(
                "개인",
                now.toLocalDate()
        );
        Set<Long> displayedPolicyIds = policies.stream()
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        if (!displayedPolicyIds.equals(agreement.displayedPolicyIds())) {
            throw new PersonalSignupException(
                    "terms",
                    "가입 약관이 변경되었습니다. 약관에 다시 동의해주세요."
            );
        }

        Set<Long> requiredPolicyIds = policies.stream()
                .filter(policy -> "필수".equals(policy.getConsentType()))
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        if (!agreement.agreedPolicyIds().containsAll(requiredPolicyIds)) {
            throw new PersonalSignupException(
                    "terms",
                    "필수 약관에 모두 동의해주세요."
            );
        }
        return policies;
    }

    private List<Long> validateDesiredJobs(List<Long> submittedIds) {
        if (submittedIds == null || submittedIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(submittedIds));
        if (distinctIds.size() > 3) {
            throw new PersonalSignupException(
                    "desiredJobIds",
                    "희망 직무는 최대 3개까지 선택할 수 있습니다."
            );
        }

        List<JobCategory> jobs = jobCategoryRepository
                .findAllByJobCategoryIdInAndIsActiveTrueAndDepth(distinctIds, 2);
        Map<Long, JobCategory> jobsById = jobs.stream()
                .collect(Collectors.toMap(
                        JobCategory::getJobCategoryId,
                        Function.identity()
                ));

        if (jobsById.size() != distinctIds.size()) {
            throw new PersonalSignupException(
                    "desiredJobIds",
                    "선택할 수 없는 희망 직무가 포함되어 있습니다."
            );
        }
        return distinctIds;
    }

    private void saveDesiredJobs(
            Long userId,
            List<Long> desiredJobIds,
            LocalDateTime now
    ) {
        List<UserDesiredJob> desiredJobs = new ArrayList<>();
        for (int index = 0; index < desiredJobIds.size(); index++) {
            desiredJobs.add(UserDesiredJob.builder()
                    .id(UserDesiredJobId.builder()
                            .userId(userId)
                            .jobCategoryId(desiredJobIds.get(index))
                            .build())
                    .displayOrder(index)
                    .createdAt(now)
                    .build());
        }
        userDesiredJobRepository.saveAll(desiredJobs);
    }

    private void savePolicyConsents(
            Long userId,
            List<Policy> policies,
            Set<Long> agreedPolicyIds,
            LocalDateTime now,
            String ipAddress,
            String userAgent
    ) {
        List<UserPolicyConsent> consents = policies.stream()
                .map(policy -> UserPolicyConsent.builder()
                        .userId(userId)
                        .policyId(policy.getPolicyId())
                        .consented(agreedPolicyIds.contains(policy.getPolicyId()))
                        .consentedAt(now)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .build())
                .toList();
        userPolicyConsentRepository.saveAll(consents);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }
}
