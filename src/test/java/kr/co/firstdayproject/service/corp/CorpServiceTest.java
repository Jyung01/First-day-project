package kr.co.firstdayproject.service.corp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.corp.CompanyAccountRequest;
import kr.co.firstdayproject.dto.corp.CompanyPasswordChangeRequest;
import kr.co.firstdayproject.dto.corp.CompanyProfileRequest;
import kr.co.firstdayproject.dto.corp.CompanyReapplyRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

class CorpServiceTest {

    private CompanyRepository companyRepository;
    private UserRepository userRepository;
    private AwsS3Service awsS3Service;
    private JobPostingRepository jobPostingRepository;
    private ApplicationRepository applicationRepository;
    private PasswordEncoder passwordEncoder;
    private CorpService corpService;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        userRepository = mock(UserRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        jobPostingRepository = mock(JobPostingRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        corpService = new CorpService(
                companyRepository,
                userRepository,
                awsS3Service,
                jobPostingRepository,
                applicationRepository,
                passwordEncoder
        );
    }

    @Test
    void loadsCompanyWithLatestRejectionReason() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(
                Company.builder()
                        .companyId(10L)
                        .approvalStatus("반려")
                        .latestRejectionReason("사업자등록번호 형식 오류")
                        .build()
        ));

        Company result = corpService.getCompany(10L);

        assertThat(result.getLatestRejectionReason())
                .isEqualTo("사업자등록번호 형식 오류");
    }

    @Test
    void rejectsMissingCompany() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> corpService.getCompany(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기업 정보를 찾을 수 없습니다");
    }

    @Test
    void loadsSignupPhoneOfCompanyManager() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(
                User.builder()
                        .userId(100L)
                        .phone("010-1234-5678")
                        .build()
        ));

        assertThat(corpService.getManagerPhone(100L))
                .isEqualTo("010-1234-5678");
    }

    @Test
    void updatesDepartmentAndPositionTitleOfCompanyManager() {
        User manager = User.builder()
                .userId(100L)
                .department("기존 부서")
                .positionTitle("기존 직책")
                .build();
        CompanyAccountRequest request = new CompanyAccountRequest();
        request.setDepartment(" 인사팀 ");
        request.setPositionTitle(" 채용 매니저 ");
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));

        corpService.updateCompanyManager(100L, request);

        assertThat(manager.getDepartment()).isEqualTo("인사팀");
        assertThat(manager.getPositionTitle()).isEqualTo("채용 매니저");
        assertThat(manager.getUpdatedAt()).isNotNull();
    }

    @Test
    void changesCompanyManagerPassword() {
        User manager = User.builder()
                .userId(100L)
                .passwordHash("old-hash")
                .build();
        CompanyPasswordChangeRequest request = new CompanyPasswordChangeRequest(
                "OldPassword!1",
                "NewPassword!2",
                "NewPassword!2"
        );
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("OldPassword!1", "old-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches("NewPassword!2", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword!2"))
                .thenReturn("new-hash");

        corpService.changePassword(100L, request);

        assertThat(manager.getPasswordHash()).isEqualTo("new-hash");
        assertThat(manager.getPasswordChangedAt()).isNotNull();
        assertThat(manager.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        User manager = User.builder()
                .userId(100L)
                .passwordHash("old-hash")
                .build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("wrong", "old-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> corpService.changePassword(
                100L,
                new CompanyPasswordChangeRequest(
                        "wrong",
                        "NewPassword!2",
                        "NewPassword!2"
                )
        ))
                .isInstanceOf(CompanyAccountException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");
    }

    @Test
    void withdrawsCompanyAndClosesActiveJobsAndApplications() {
        User manager = User.builder()
                .userId(100L)
                .companyId(10L)
                .loginId("corp-manager")
                .email("manager@example.com")
                .name("김철수")
                .phone("010-1234-5678")
                .department("인사팀")
                .positionTitle("과장")
                .passwordHash("password-hash")
                .accountStatus("정상")
                .build();
        Company company = Company.builder()
                .companyId(10L)
                .businessNumber("1112233333")
                .companyName("첫출근 주식회사")
                .representativeName("이대표")
                .companyStatus("정상")
                .build();
        when(userRepository.findById(100L)).thenReturn(Optional.of(manager));
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(passwordEncoder.matches("Password!1", "password-hash"))
                .thenReturn(true);

        corpService.withdrawCompany(100L, 10L, "Password!1");

        assertThat(manager.getAccountStatus()).isEqualTo("탈퇴");
        assertThat(manager.getWithdrawnAt()).isNotNull();
        assertThat(company.getCompanyStatus()).isEqualTo("탈퇴");
        assertThat(company.getWithdrawnAt()).isNotNull();
        // 담당자 개인식별정보는 마스킹된다.
        assertThat(manager.getName()).isEqualTo("탈퇴한 담당자");
        assertThat(manager.getPhone()).isNull();
        assertThat(manager.getDepartment()).isNull();
        assertThat(manager.getPositionTitle()).isNull();
        // 재가입 중복 방지 키와 법인 정보는 남는다.
        assertThat(manager.getLoginId()).isEqualTo("corp-manager");
        assertThat(manager.getEmail()).isEqualTo("manager@example.com");
        assertThat(company.getCompanyName()).isEqualTo("첫출근 주식회사");
        assertThat(company.getBusinessNumber()).isEqualTo("1112233333");
        assertThat(company.getRepresentativeName()).isEqualTo("이대표");
        verify(applicationRepository)
                .recordCompanyWithdrawalStatusHistory(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.argThat(statuses ->
                                !statuses.contains("최종합격")
                        ),
                        org.mockito.ArgumentMatchers.any()
                );
        verify(applicationRepository)
                .terminateActiveApplicationsForCompanyWithdrawal(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.argThat(statuses ->
                                !statuses.contains("최종합격")
                        ),
                        org.mockito.ArgumentMatchers.any()
                );
        verify(jobPostingRepository).closeRecruitingPostingsForWithdrawal(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void countsWithdrawalSummaryWithoutLoadingJsonEntities() {
        when(jobPostingRepository.countByCompanyIdAndStatusIn(10L, List.of("모집중", "모집예정")))
                .thenReturn(2L);
        when(applicationRepository.countActiveApplicantsOfCompany(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyCollection()
        )).thenReturn(5L);

        var summary = corpService.getWithdrawalSummary(10L);

        assertThat(summary.activeJobCount()).isEqualTo(2L);
        assertThat(summary.activeApplicantCount()).isEqualTo(5L);
    }

    @Test
    void updatesCompanyAndChangesStatusToPendingWhenReapprovalIsRequested() {
        Company company = Company.builder()
                .companyId(10L)
                .businessNumber("1112233333")
                .companyName("수정 전 기업")
                .approvalStatus("반려")
                .latestRejectionCode("MISSING_INFORMATION")
                .latestRejectionReason("필수 정보를 확인해주세요.")
                .build();
        CompanyReapplyRequest request = validReapplyRequest();
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByBusinessNumberInAndCompanyIdNot(
                List.of("1234567890", "123-45-67890"),
                10L
        )).thenReturn(false);

        corpService.requestReapproval(10L, request, null);

        assertThat(company.getCompanyName()).isEqualTo("수정한 기업");
        assertThat(company.getBusinessNumber()).isEqualTo("1234567890");
        assertThat(company.getRepresentativeName()).isEqualTo("김대표");
        assertThat(company.getApprovalStatus()).isEqualTo("승인대기");
        assertThat(company.getReapplyRequestedAt()).isNotNull();
        assertThat(company.getUpdatedAt()).isNotNull();
        assertThat(company.getLatestRejectionCode())
                .isEqualTo("MISSING_INFORMATION");
        assertThat(company.getLatestRejectionReason())
                .isEqualTo("필수 정보를 확인해주세요.");
        verify(companyRepository).existsByBusinessNumberInAndCompanyIdNot(
                List.of("1234567890", "123-45-67890"),
                10L
        );
    }

    @Test
    void rejectsBusinessNumberOwnedByAnotherCompany() {
        Company company = Company.builder()
                .companyId(10L)
                .approvalStatus("반려")
                .build();
        CompanyReapplyRequest request = validReapplyRequest();
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByBusinessNumberInAndCompanyIdNot(
                List.of("1234567890", "123-45-67890"),
                10L
        )).thenReturn(true);

        assertThatThrownBy(() ->
                corpService.requestReapproval(10L, request, null)
        )
                .isInstanceOf(CompanyReapplyException.class)
                .hasMessage("이미 등록된 사업자등록번호입니다.");

        assertThat(company.getApprovalStatus()).isEqualTo("반려");
        assertThat(company.getReapplyRequestedAt()).isNull();
    }

    @Test
    void updatesApprovedCompanyProfileWithoutChangingApprovalStatus() {
        Company company = Company.builder()
                .companyId(10L)
                .approvalStatus("승인")
                .companyName("수정 전 기업")
                .build();
        CompanyProfileRequest request = new CompanyProfileRequest();
        request.setCompanyName("수정한 기업");
        request.setRepresentativeName("김대표");
        request.setEstablishedDate(LocalDate.of(2020, 1, 2));
        request.setCompanySize("중소기업");
        request.setIndustry("IT·인터넷");
        request.setHomepage("https://example.com");
        request.setPostcode("06236");
        request.setAddress("서울특별시 강남구 테헤란로 123");
        request.setAddressDetail("4층");
        request.setShortDescription("기업 한 줄 소개");
        request.setBenefits(List.of("복지 정보"));
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        corpService.updateCompanyProfile(10L, request, null);

        assertThat(company.getCompanyName()).isEqualTo("수정한 기업");
        assertThat(company.getRepresentativeName()).isEqualTo("김대표");
        assertThat(company.getPostalCode()).isEqualTo("06236");
        assertThat(company.getAddressLine1())
                .isEqualTo("서울특별시 강남구 테헤란로 123");
        assertThat(company.getAddressLine2()).isEqualTo("4층");
        assertThat(company.getCompanySize()).isEqualTo("중소기업");
        assertThat(company.getApprovalStatus()).isEqualTo("승인");
        assertThat(company.getUpdatedAt()).isNotNull();
    }

    @Test
    void uploadsChangedLogoWhenReapprovalIsRequested() throws Exception {
        Company company = Company.builder()
                .companyId(10L)
                .approvalStatus("반려")
                .logoUrl("https://cdn.test/companies_logo/previous.png")
                .build();
        CompanyReapplyRequest request = validReapplyRequest();
        MockMultipartFile logo = new MockMultipartFile(
                "companyLogo",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByBusinessNumberInAndCompanyIdNot(
                List.of("1234567890", "123-45-67890"),
                10L
        )).thenReturn(false);
        when(awsS3Service.upload(logo, "companies_logo"))
                .thenReturn("https://cdn.test/companies_logo/logo.png");

        corpService.requestReapproval(10L, request, logo);

        assertThat(company.getLogoUrl())
                .isEqualTo("https://cdn.test/companies_logo/logo.png");
        assertThat(company.getApprovalStatus()).isEqualTo("승인대기");
        verify(awsS3Service).upload(logo, "companies_logo");
        verify(awsS3Service).synchronizePublicReplacement(
                "https://cdn.test/companies_logo/previous.png",
                "https://cdn.test/companies_logo/logo.png"
        );
    }

    private CompanyReapplyRequest validReapplyRequest() {
        CompanyReapplyRequest request = new CompanyReapplyRequest();
        request.setCompanyName("수정한 기업");
        request.setBusinessNumber("123-45-67890");
        request.setRepresentativeName("김대표");
        request.setEstablishedDate(LocalDate.of(2020, 1, 2));
        request.setCompanySize("중소기업");
        request.setIndustry("IT·인터넷");
        request.setHomepage("https://example.com");
        request.setShortDescription("기업 한 줄 소개");
        request.setBenefits(List.of("복지 정보"));
        return request;
    }
}
