package kr.co.firstdayproject.controller.corp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import kr.co.firstdayproject.dto.corp.CompanyAccountRequest;
import kr.co.firstdayproject.dto.corp.CompanyAccountActionResponse;
import kr.co.firstdayproject.dto.corp.CompanyPasswordChangeRequest;
import kr.co.firstdayproject.dto.corp.CompanyWithdrawalRequest;
import kr.co.firstdayproject.dto.corp.CompanyWithdrawalSummary;
import kr.co.firstdayproject.dto.corp.CompanyProfileRequest;
import kr.co.firstdayproject.dto.corp.CompanyReapplyRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.corp.CorpService;
import kr.co.firstdayproject.service.corp.CorpDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;

class CorpControllerTest {

    private CorpService corpService;
    private CorpDashboardService corpDashboardService;
    private CorpController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        corpService = mock(CorpService.class);
        corpDashboardService = mock(CorpDashboardService.class);
        controller = new CorpController(corpService, corpDashboardService);
        model = mock(Model.class);
    }

    @Test
    void redirectsRejectedCompanyDashboardToCorrectionPage() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        when(corpService.getCompany(10L)).thenReturn(rejectedCompany(10L));

        String view = controller.index(userDetails, model);

        assertThat(view).isEqualTo("redirect:/corp/company-info-rejected");
    }

    @Test
    void redirectsRejectedCompanyInfoToCorrectionPage() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        when(corpService.getCompany(10L)).thenReturn(rejectedCompany(10L));

        String view = controller.companyInfo(userDetails, model);

        assertThat(view).isEqualTo("redirect:/corp/company-info-rejected");
    }

    @Test
    void allowsApprovedCompanyDashboard() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        when(corpService.getCompany(10L)).thenReturn(Company.builder()
                .companyId(10L)
                .approvalStatus("승인")
                .build());

        String view = controller.index(userDetails, model);

        assertThat(view).isEqualTo("corp/index");
    }

    @Test
    void submitsReapprovalAndRedirectsToCompletionModal() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        CompanyReapplyRequest request = new CompanyReapplyRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "companyForm");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(corpService.getCompany(10L)).thenReturn(rejectedCompany(10L));
        when(httpRequest.getSession(false)).thenReturn(session);

        String view = controller.requestCompanyReapproval(
                userDetails,
                request,
                bindingResult,
                null,
                model,
                httpRequest
        );

        assertThat(view)
                .isEqualTo("redirect:/auth/login?companyApproval=reapplied");
        verify(corpService).requestReapproval(10L, request, null);
        verify(session).invalidate();
    }

    @Test
    void loadsCurrentCompanyInformation() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        Company company = Company.builder()
                .companyId(10L)
                .companyName("첫출근랩")
                .businessNumber("1234567890")
                .approvalStatus("승인")
                .build();
        when(corpService.getCompany(10L)).thenReturn(company);
        when(corpService.getManagerPhone(100L)).thenReturn("010-1234-5678");

        String view = controller.companyInfo(userDetails, model);

        assertThat(view).isEqualTo("corp/company-info");
        verify(model).addAttribute(
                org.mockito.ArgumentMatchers.eq("companyForm"),
                org.mockito.ArgumentMatchers.any(CompanyProfileRequest.class)
        );
        verify(model).addAttribute("businessNumber", "123-45-67890");
        verify(model).addAttribute("managerPhone", "010-1234-5678");
    }

    @Test
    void savesCompanyInformation() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        Company company = Company.builder()
                .companyId(10L)
                .approvalStatus("승인")
                .build();
        CompanyProfileRequest request = new CompanyProfileRequest();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "companyForm");
        when(corpService.getCompany(10L)).thenReturn(company);

        String view = controller.updateCompanyInfo(
                userDetails,
                request,
                bindingResult,
                null,
                model
        );

        assertThat(view).isEqualTo("redirect:/corp/company-info?saved=true");
        verify(corpService).updateCompanyProfile(10L, request, null);
    }

    @Test
    void loadsAccountInformationFromLoggedInUser() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        User manager = User.builder()
                .userId(100L)
                .loginId("company08")
                .name("기업 담당자")
                .email("manager@example.com")
                .phone("010-1234-5678")
                .department("인사팀")
                .positionTitle("채용 매니저")
                .passwordChangedAt(LocalDateTime.of(2026, 8, 1, 12, 0))
                .build();
        when(corpService.getCompany(10L)).thenReturn(Company.builder()
                .companyId(10L)
                .approvalStatus("승인")
                .build());
        when(corpService.getCompanyManager(100L)).thenReturn(manager);
        when(corpService.getWithdrawalSummary(10L))
                .thenReturn(new CompanyWithdrawalSummary(2L, 5L));

        String view = controller.account(userDetails, model);

        assertThat(view).isEqualTo("corp/account");
        verify(model).addAttribute("loginId", "company08");
        verify(model).addAttribute("managerName", "기업 담당자");
        verify(model).addAttribute("managerEmail", "manager@example.com");
        verify(model).addAttribute("managerPhone", "010-1234-5678");
        verify(model).addAttribute("passwordChangedAt", "2026.08.01");
        verify(model).addAttribute("activeJobCount", 2L);
        verify(model).addAttribute("activeApplicantCount", 5L);
        verify(model).addAttribute(
                org.mockito.ArgumentMatchers.eq("accountForm"),
                org.mockito.ArgumentMatchers.any(CompanyAccountRequest.class)
        );
    }

    @Test
    void savesDepartmentAndPositionTitle() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        CompanyAccountRequest request = new CompanyAccountRequest();
        request.setDepartment("인사팀");
        request.setPositionTitle("채용 매니저");
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(request, "accountForm");
        when(corpService.getCompany(10L)).thenReturn(Company.builder()
                .companyId(10L)
                .approvalStatus("승인")
                .build());
        when(corpService.getCompanyManager(100L)).thenReturn(User.builder()
                .userId(100L)
                .build());

        String view = controller.updateAccount(
                userDetails,
                request,
                bindingResult,
                model
        );

        assertThat(view).isEqualTo("redirect:/corp/account?saved=true");
        verify(corpService).updateCompanyManager(100L, request);
    }

    @Test
    void changesPasswordThroughAccountApi() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        CompanyPasswordChangeRequest request = new CompanyPasswordChangeRequest(
                "OldPassword!1",
                "NewPassword!2",
                "NewPassword!2"
        );

        ResponseEntity<CompanyAccountActionResponse> response =
                controller.changePassword(userDetails, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        verify(corpService).changePassword(100L, request);
    }

    @Test
    void withdrawsAccountAndInvalidatesSession() {
        CustomUserDetails userDetails = companyUserDetails(10L);
        CompanyWithdrawalRequest request =
                new CompanyWithdrawalRequest("Password!1");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(false)).thenReturn(session);

        ResponseEntity<CompanyAccountActionResponse> response =
                controller.withdrawAccount(userDetails, request, httpRequest);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().redirectUrl())
                .isEqualTo("/auth/login?accountStatus=withdrawn");
        verify(corpService).withdrawCompany(100L, 10L, "Password!1");
        verify(session).invalidate();
    }

    private CustomUserDetails companyUserDetails(Long companyId) {
        User user = User.builder()
                .userId(100L)
                .companyId(companyId)
                .loginId("company08")
                .passwordHash("encoded-password")
                .name("기업 담당자")
                .userType("기업")
                .build();
        return new CustomUserDetails(user, "COMPANY", true);
    }

    private Company rejectedCompany(Long companyId) {
        return Company.builder()
                .companyId(companyId)
                .approvalStatus("반려")
                .build();
    }
}
