package kr.co.firstdayproject.service.corp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.co.firstdayproject.dto.corp.CompanyAccountRequest;
import kr.co.firstdayproject.dto.corp.CompanyPasswordChangeRequest;
import kr.co.firstdayproject.dto.corp.CompanyProfileRequest;
import kr.co.firstdayproject.dto.corp.CompanyReapplyRequest;
import kr.co.firstdayproject.dto.corp.CompanyWithdrawalSummary;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpService {

    private static final String REJECTED_APPROVAL = "반려";
    private static final String PENDING_APPROVAL = "승인대기";
    private static final String WITHDRAWN_STATUS = "탈퇴";
    private static final List<String> ACTIVE_APPLICATION_STATUSES = List.of(
            "지원완료", "서류검토중", "서류합격", "면접예정", "면접완료", "최종합격"
    );
    private static final java.util.regex.Pattern PASSWORD_PATTERN =
            java.util.regex.Pattern.compile(
                    "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
            );

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException(
                        "기업 정보를 찾을 수 없습니다."
                ));
    }

    public String getManagerPhone(Long userId) {
        return getCompanyManager(userId).getPhone();
    }

    public User getCompanyManager(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "기업 담당자 정보를 찾을 수 없습니다."
                ));
    }

    @Transactional
    public void updateCompanyManager(
            Long userId,
            CompanyAccountRequest request
    ) {
        User manager = getCompanyManager(userId);
        manager.setDepartment(trimToNull(request.getDepartment()));
        manager.setPositionTitle(trimToNull(request.getPositionTitle()));
        manager.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void changePassword(
            Long userId,
            CompanyPasswordChangeRequest request
    ) {
        User manager = getCompanyManager(userId);
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new CompanyAccountException("현재 비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, manager.getPasswordHash())) {
            throw new CompanyAccountException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new CompanyAccountException(
                    "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
        }
        if (!newPassword.equals(request.newPasswordConfirm())) {
            throw new CompanyAccountException(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
        if (passwordEncoder.matches(newPassword, manager.getPasswordHash())) {
            throw new CompanyAccountException(
                    "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        manager.setPasswordHash(passwordEncoder.encode(newPassword));
        manager.setPasswordChangedAt(now);
        manager.setUpdatedAt(now);
    }

    public CompanyWithdrawalSummary getWithdrawalSummary(Long companyId) {
        long activeJobCount = jobPostingRepository
                .countByCompanyIdAndStatus(companyId, "모집중");
        long applicantCount = activeJobCount == 0L
                ? 0L
                : applicationRepository
                        .countActiveApplicantsOfRecruitingCompany(
                                companyId,
                                ACTIVE_APPLICATION_STATUSES
                        );
        return new CompanyWithdrawalSummary(activeJobCount, applicantCount);
    }

    @Transactional
    public void withdrawCompany(
            Long userId,
            Long companyId,
            String currentPassword
    ) {
        User manager = getCompanyManager(userId);
        Company company = getCompany(companyId);
        if (!Objects.equals(manager.getCompanyId(), companyId)) {
            throw new CompanyAccountException("기업 계정 정보를 확인할 수 없습니다.");
        }
        if (WITHDRAWN_STATUS.equals(manager.getAccountStatus())
                || WITHDRAWN_STATUS.equals(company.getCompanyStatus())) {
            throw new CompanyAccountException("이미 탈퇴 처리된 기업회원입니다.");
        }
        if (currentPassword == null
                || !passwordEncoder.matches(
                        currentPassword,
                        manager.getPasswordHash()
                )) {
            throw new CompanyAccountException("현재 비밀번호가 일치하지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        applicationRepository.terminateActiveApplicationsForCompanyWithdrawal(
                companyId,
                ACTIVE_APPLICATION_STATUSES,
                now
        );
        jobPostingRepository.closeRecruitingPostingsForWithdrawal(
                companyId,
                now
        );

        manager.setAccountStatus(WITHDRAWN_STATUS);
        manager.setWithdrawnAt(now);
        manager.setUpdatedAt(now);
        company.setCompanyStatus(WITHDRAWN_STATUS);
        company.setWithdrawnAt(now);
        company.setUpdatedAt(now);
    }


    @Transactional
    public void updateCompanyProfile(
            Long companyId,
            CompanyProfileRequest request,
            MultipartFile companyLogo
    ) {
        Company company = getCompany(companyId);
        if (REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            throw new CompanyProfileUpdateException(
                    null,
                    "반려 상태에서는 재심사 페이지에서 기업정보를 수정해주세요."
            );
        }

        if (companyLogo != null && !companyLogo.isEmpty()) {
            validateCompanyLogo(companyLogo);
            try {
                company.setLogoUrl(
                        awsS3Service.upload(companyLogo, "companies_logo")
                );
            } catch (IOException | RuntimeException exception) {
                throw new CompanyProfileUpdateException(
                        "companyLogo",
                        "기업 로고를 업로드하지 못했습니다. 다시 시도해주세요.",
                        exception
                );
            }
        }

        company.setCompanyName(request.getCompanyName().trim());
        company.setRepresentativeName(request.getRepresentativeName().trim());
        company.setEstablishedDate(request.getEstablishedDate());
        company.setCompanySize(trimToNull(request.getCompanySize()));
        company.setIndustryName(trimToNull(request.getIndustry()));
        company.setHomepageUrl(trimToNull(request.getHomepage()));
        company.setPostalCode(request.getPostcode().trim());
        company.setAddressLine1(request.getAddress().trim());
        company.setAddressLine2(trimToNull(request.getAddressDetail()));
        company.setShortDescription(trimToNull(request.getShortDescription()));
        company.setBenefits(trimToNull(request.getBenefits()));
        company.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void requestReapproval(
            Long companyId,
            CompanyReapplyRequest request,
            MultipartFile companyLogo
    ) {
        Company company = getCompany(companyId);
        if (!REJECTED_APPROVAL.equals(company.getApprovalStatus())) {
            throw new CompanyReapplyException(
                    null,
                    "반려 상태인 기업만 재심사를 요청할 수 있습니다."
            );
        }

        String businessNumber = normalizeBusinessNumber(request.getBusinessNumber());
        if (companyRepository.existsByBusinessNumberInAndCompanyIdNot(
                businessNumberCandidates(businessNumber),
                companyId
        )) {
            throw new CompanyReapplyException(
                    "businessNumber",
                    "이미 등록된 사업자등록번호입니다."
            );
        }

        if (companyLogo != null && !companyLogo.isEmpty()) {
            try {
                validateCompanyLogo(companyLogo);
                company.setLogoUrl(
                        awsS3Service.upload(companyLogo, "companies_logo")
                );
            } catch (CompanyProfileUpdateException exception) {
                throw new CompanyReapplyException(
                        "companyLogo",
                        exception.getMessage()
                );
            } catch (IOException | RuntimeException exception) {
                throw new CompanyReapplyException(
                        "companyLogo",
                        "기업 로고를 업로드하지 못했습니다. 다시 시도해주세요."
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();
        company.setCompanyName(request.getCompanyName().trim());
        company.setBusinessNumber(businessNumber);
        company.setRepresentativeName(request.getRepresentativeName().trim());
        company.setEstablishedDate(request.getEstablishedDate());
        company.setCompanySize(trimToNull(request.getCompanySize()));
        company.setIndustryName(trimToNull(request.getIndustry()));
        company.setHomepageUrl(trimToNull(request.getHomepage()));
        company.setShortDescription(trimToNull(request.getShortDescription()));
        company.setBenefits(trimToNull(request.getBenefits()));
        company.setApprovalStatus(PENDING_APPROVAL);
        company.setReapplyRequestedAt(now);
        company.setUpdatedAt(now);
    }

    private String normalizeBusinessNumber(String businessNumber) {
        return businessNumber.replace("-", "").trim();
    }

    private List<String> businessNumberCandidates(String businessNumber) {
        return List.of(
                businessNumber,
                businessNumber.substring(0, 3)
                        + "-" + businessNumber.substring(3, 5)
                        + "-" + businessNumber.substring(5)
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateCompanyLogo(MultipartFile companyLogo) {
        Set<String> allowedContentTypes = Set.of("image/jpeg", "image/png");
        if (!allowedContentTypes.contains(companyLogo.getContentType())) {
            throw new CompanyProfileUpdateException(
                    "companyLogo",
                    "JPG 또는 PNG 이미지만 등록할 수 있습니다."
            );
        }
        if (companyLogo.getSize() > 5L * 1024 * 1024) {
            throw new CompanyProfileUpdateException(
                    "companyLogo",
                    "기업 로고는 5MB 이하로 등록해주세요."
            );
        }
    }
}
