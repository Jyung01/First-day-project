package kr.co.firstdayproject.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.auth.LoginAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginSuccessHandlerTest {

    private CompanyRepository companyRepository;
    private LoginAuditService loginAuditService;
    private LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        loginAuditService = mock(LoginAuditService.class);
        handler = new LoginSuccessHandler(companyRepository, loginAuditService);
    }

    @Test
    void routesPersonalUserToMain() {
        assertThat(handler.resolveDestination(authentication("ROLE_PERSONAL"))).isEqualTo("/");
    }

    @Test
    void routesCompanyUserToCompanyDashboard() {
        assertThat(handler.resolveDestination(authentication("ROLE_COMPANY"))).isEqualTo("/corp");
    }

    @Test
    void routesAdminUserToAdminDashboard() {
        assertThat(handler.resolveDestination(authentication("ROLE_ADMIN"))).isEqualTo("/admin");
    }

    @Test
    void rejectsPendingCompanyLoginAfterPasswordAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Authentication authentication = companyAuthentication(10L);

        when(request.getSession(false)).thenReturn(session);
        when(request.getContextPath()).thenReturn("");
        when(companyRepository.findById(10L)).thenReturn(Optional.of(
                Company.builder()
                        .companyId(10L)
                        .approvalStatus("승인대기")
                        .build()
        ));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(session).invalidate();
        verify(loginAuditService, never()).recordSuccessfulLogin(100L);
        verify(response).sendRedirect("/auth/login?companyApproval=pending");
    }

    @Test
    void allowsApprovedCompanyLogin() {
        Authentication authentication = companyAuthentication(10L);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(
                Company.builder()
                        .companyId(10L)
                        .approvalStatus("승인")
                        .build()
        ));

        assertThat(handler.isPendingCompany(authentication)).isFalse();
    }

    @Test
    void routesRejectedCompanyToCorrectionPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = companyAuthentication(10L);

        when(request.getContextPath()).thenReturn("");
        when(companyRepository.findById(10L)).thenReturn(Optional.of(
                Company.builder()
                        .companyId(10L)
                        .approvalStatus("반려")
                        .latestRejectionReason("사업자등록번호 형식 오류")
                        .build()
        ));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(loginAuditService).recordSuccessfulLogin(100L);
        verify(response).sendRedirect(
                "/corp/company-info-rejected?showRejectionModal=true"
        );
    }

    @Test
    void recordsLastLoginBeforeRedirectingPersonalUser() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = userAuthentication("PERSONAL");

        when(request.getContextPath()).thenReturn("");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(loginAuditService).recordSuccessfulLogin(100L);
        verify(response).sendRedirect("/");
    }

    @Test
    void continuesLoginWhenLastLoginUpdateFails() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = userAuthentication("PERSONAL");

        when(request.getContextPath()).thenReturn("");
        doThrow(new IllegalStateException("database error"))
                .when(loginAuditService)
                .recordSuccessfulLogin(100L);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("/");
    }

    private Authentication authentication(String role) {
        return new UsernamePasswordAuthenticationToken(
                "loginId",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private Authentication companyAuthentication(Long companyId) {
        User user = User.builder()
                .userId(100L)
                .companyId(companyId)
                .loginId("company01")
                .passwordHash("encoded-password")
                .name("기업 담당자")
                .userType("기업")
                .build();
        CustomUserDetails principal = new CustomUserDetails(
                user,
                "COMPANY",
                true
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );
    }

    private Authentication userAuthentication(String role) {
        User user = User.builder()
                .userId(100L)
                .loginId("personal01")
                .passwordHash("encoded-password")
                .name("personal user")
                .userType("PERSONAL")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user, role, true);
        return new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );
    }
}
