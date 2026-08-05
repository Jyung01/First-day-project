package kr.co.firstdayproject.service.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.CorporateSignupRequest;
import kr.co.firstdayproject.dto.auth.CorporateTermsAgreement;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.entity.policy.UserPolicyConsent;
import kr.co.firstdayproject.repository.company.CompanyRepository;
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
public class CorporateSignupService {

    private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{6,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
    );
    private static final Pattern BUSINESS_NUMBER_PATTERN =
            Pattern.compile("^\\d{3}-?\\d{2}-?\\d{5}$");

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
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

    public boolean isBusinessNumberAvailable(String businessNumber) {
        if (businessNumber == null) {
            return false;
        }

        String trimmedBusinessNumber = businessNumber.trim();
        if (!BUSINESS_NUMBER_PATTERN.matcher(trimmedBusinessNumber).matches()) {
            return false;
        }

        return !businessNumberExists(normalizeBusinessNumber(trimmedBusinessNumber));
    }

    @Transactional
    public Long signup(
            CorporateSignupRequest request,
            CorporateTermsAgreement termsAgreement,
            VerifiedEmail verifiedEmail,
            String ipAddress,
            String userAgent
    ) {
        String loginId = request.getMemberId().trim();
        String email = normalizeEmail(request.getManagerEmail());
        String businessNumber = normalizeBusinessNumber(
                request.getBusinessNumber().trim()
        );

        validateAccount(request, loginId, email, verifiedEmail);
        if (businessNumberExists(businessNumber)) {
            throw new CorporateSignupException(
                    "businessNumber",
                    "이미 등록된 사업자등록번호입니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        List<Policy> policies = validateTermsAgreement(termsAgreement, now);

        Company company = Company.builder()
                .businessNumber(businessNumber)
                .companyName(request.getCompanyName().trim())
                .industryName(request.getIndustry().trim())
                .companySize(request.getCompanySize().trim())
                .postalCode(request.getPostcode().trim())
                .addressLine1(request.getAddress().trim())
                .addressLine2(trimToNull(request.getAddressDetail()))
                .approvalStatus("승인대기")
                .companyStatus("정상")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Company savedCompany;
        try {
            savedCompany = companyRepository.saveAndFlush(company);
        } catch (DataIntegrityViolationException exception) {
            throw new CorporateSignupException(
                    "businessNumber",
                    "이미 등록된 사업자등록번호입니다."
            );
        }

        User user = User.builder()
                .companyId(savedCompany.getCompanyId())
                .loginId(loginId)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getManagerName().trim())
                .email(email)
                .phone(request.getManagerPhone().trim())
                .userType("기업")
                .accountStatus("정상")
                .passwordChangedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new CorporateSignupException(
                    null,
                    "이미 사용 중인 아이디 또는 이메일입니다."
            );
        }

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

    public String normalizeBusinessNumber(String businessNumber) {
        return businessNumber.replace("-", "");
    }

    private void validateAccount(
            CorporateSignupRequest request,
            String loginId,
            String email,
            VerifiedEmail verifiedEmail
    ) {
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new CorporateSignupException(
                    "memberId",
                    "아이디는 영문과 숫자 6~20자로 입력해주세요."
            );
        }
        if (userRepository.existsByLoginIdIgnoreCase(loginId)) {
            throw new CorporateSignupException(
                    "memberId",
                    "이미 사용 중인 아이디입니다."
            );
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new CorporateSignupException(
                    "managerEmail",
                    "이미 가입된 이메일입니다."
            );
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new CorporateSignupException(
                    "password",
                    "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CorporateSignupException(
                    "passwordConfirm",
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
        if (!isVerifiedEmail(email, verifiedEmail)) {
            throw new CorporateSignupException(
                    "managerEmail",
                    "이메일 인증이 만료되었거나 인증한 이메일과 일치하지 않습니다."
            );
        }
    }

    private List<Policy> validateTermsAgreement(
            CorporateTermsAgreement agreement,
            LocalDateTime now
    ) {
        if (agreement == null) {
            throw new CorporateSignupException("terms", "약관 동의 정보가 없습니다.");
        }

        List<Policy> policies = policyRepository.findActiveSignupPolicies("기업", now);
        Set<Long> displayedPolicyIds = policies.stream()
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        if (!displayedPolicyIds.equals(agreement.displayedPolicyIds())) {
            throw new CorporateSignupException(
                    "terms",
                    "가입 약관이 변경되었습니다. 약관에 다시 동의해주세요."
            );
        }

        Set<Long> requiredPolicyIds = policies.stream()
                .filter(policy -> "필수".equals(policy.getConsentType()))
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        if (!agreement.agreedPolicyIds().containsAll(requiredPolicyIds)) {
            throw new CorporateSignupException(
                    "terms",
                    "필수 약관에 모두 동의해주세요."
            );
        }
        return policies;
    }

    private boolean businessNumberExists(String normalizedBusinessNumber) {
        return companyRepository.existsByBusinessNumberIn(List.of(
                normalizedBusinessNumber,
                formatBusinessNumber(normalizedBusinessNumber)
        ));
    }

    private String formatBusinessNumber(String businessNumber) {
        return businessNumber.substring(0, 3)
                + "-" + businessNumber.substring(3, 5)
                + "-" + businessNumber.substring(5);
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
