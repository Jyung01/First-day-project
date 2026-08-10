package kr.co.firstdayproject.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.auth.LoginAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String PENDING_APPROVAL = "승인대기";
    private static final String REJECTED_APPROVAL = "반려";
    private static final String PENDING_LOGIN_URL =
            "/auth/login?companyApproval=pending";
    private static final String REJECTED_COMPANY_URL =
            "/corp/company-info-rejected?showRejectionModal=true";

    private final CompanyRepository companyRepository;
    private final LoginAuditService loginAuditService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Optional<String> companyApprovalStatus =
                findCompanyApprovalStatus(authentication);

        if (companyApprovalStatus.filter(PENDING_APPROVAL::equals).isPresent()) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(
                    request.getContextPath() + PENDING_LOGIN_URL
            );
            return;
        }

        recordLastLogin(authentication);

        if (companyApprovalStatus.filter(REJECTED_APPROVAL::equals).isPresent()) {
            response.sendRedirect(
                    request.getContextPath() + REJECTED_COMPANY_URL
            );
            return;
        }

        String destination = resolveDestination(authentication);
        response.sendRedirect(request.getContextPath() + destination);
    }

    boolean isPendingCompany(Authentication authentication) {
        return findCompanyApprovalStatus(authentication)
                .filter(PENDING_APPROVAL::equals)
                .isPresent();
    }

    Optional<String> findCompanyApprovalStatus(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_COMPANY")
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || userDetails.getCompanyId() == null) {
            return Optional.empty();
        }

        return companyRepository.findById(userDetails.getCompanyId())
                .map(Company::getApprovalStatus);
    }

    String resolveDestination(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin";
        }
        if (hasRole(authentication, "ROLE_COMPANY")) {
            return "/corp";
        }
        return "/";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private void recordLastLogin(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return;
        }

        try {
            loginAuditService.recordSuccessfulLogin(userDetails.getUserId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to update last login time. userId={}",
                    userDetails.getUserId(),
                    exception
            );
        }
    }
}
