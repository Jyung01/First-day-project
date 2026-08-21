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
    /** 가입 후 기업정보를 아직 작성 중인 기업이 로그인하면 보내는 곳. */
    private static final String DRAFT_COMPANY_URL =
            "/corp/company-info?reviewDraft=true";

    private final CompanyRepository companyRepository;
    private final LoginAuditService loginAuditService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        Optional<Company> company = findCompany(authentication);
        Optional<String> companyApprovalStatus = company.map(Company::getApprovalStatus);

        /*
         * 승인대기라도 아직 심사를 요청하지 않았다면(reviewRequestedAt == null) 로그인시킨다.
         * 가입 단계에서는 대표자명·설립일·기업소개 등이 비어 있어, 로그인해서 채운 뒤
         * 직접 심사를 요청해야 하기 때문이다. 이때 갈 곳은 기업정보 화면뿐이며,
         * 나머지 기업 화면은 CompanyReviewGateInterceptor가 막는다.
         *
         * 이미 심사를 요청한 기업은 종전대로 로그인을 막는다. 심사 중에는 할 수 있는 일이 없고,
         * 정보가 바뀌면 관리자가 심사한 내용과 승인된 내용이 달라지기 때문이다.
         */
        if (companyApprovalStatus.filter(PENDING_APPROVAL::equals).isPresent()) {
            boolean draft = company
                    .map(Company::getReviewRequestedAt)
                    .isEmpty();

            if (draft) {
                recordLastLogin(authentication);
                response.sendRedirect(
                        request.getContextPath() + DRAFT_COMPANY_URL
                );
                return;
            }

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

        String destination = resolveDestination(request, authentication);
        response.sendRedirect(request.getContextPath() + destination);
    }

    boolean isPendingCompany(Authentication authentication) {
        return findCompanyApprovalStatus(authentication)
                .filter(PENDING_APPROVAL::equals)
                .isPresent();
    }

    Optional<String> findCompanyApprovalStatus(Authentication authentication) {
        return findCompany(authentication).map(Company::getApprovalStatus);
    }

    private Optional<Company> findCompany(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_COMPANY")
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || userDetails.getCompanyId() == null) {
            return Optional.empty();
        }

        return companyRepository.findById(userDetails.getCompanyId());
    }

    String resolveDestination(HttpServletRequest request,
                              Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin";
        }

        if (hasRole(authentication, "ROLE_COMPANY")) {
            return "/corp";
        }

        String returnUrl = request.getParameter("returnUrl");

        if (isSafeReturnUrl(returnUrl)) {
            return returnUrl;
        }

        return "/";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private boolean isSafeReturnUrl(String returnUrl) {
        return returnUrl != null
                && !returnUrl.isBlank()
                && returnUrl.startsWith("/")
                && !returnUrl.startsWith("//")
                && !returnUrl.contains("\r")
                && !returnUrl.contains("\n");
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
