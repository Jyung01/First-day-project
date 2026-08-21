package kr.co.firstdayproject.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 가입 후 기업정보를 아직 작성 중인 기업(승인대기 + reviewRequestedAt == null)이
 * 기업정보 화면 밖으로 나가지 못하게 막는다.
 *
 * <p>가입 단계에서 채워지는 값은 사업자번호·기업명·업종·규모·주소뿐이다.
 * 나머지를 채우고 심사를 요청해야 관리자 심사 큐에 올라가므로, 그 전까지는
 * 공고 등록·지원자 관리 같은 화면을 열어봐야 할 일이 없다.
 *
 * <p>심사를 요청한 뒤에는 {@code LoginSuccessHandler}가 로그인 자체를 막으므로
 * 이 인터셉터까지 오지 않는다.
 */
@RequiredArgsConstructor
public class CompanyReviewGateInterceptor implements HandlerInterceptor {

    private static final String PENDING_APPROVAL = "승인대기";

    /** 작성 중에도 열어두는 경로. 기업정보 작성과 계정 관리, 로그아웃은 막지 않는다. */
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/corp/company-info",
            "/corp/account",
            "/corp/logout"
    );

    private static final String REDIRECT_TARGET = "/corp/company-info?reviewDraft=true";

    private final CompanyRepository companyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        if (isAllowed(path)) {
            return true;
        }

        if (!isDraftCompany()) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + REDIRECT_TARGET);
        return false;
    }

    private boolean isAllowed(String path) {
        return ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isDraftCompany() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)
                || userDetails.getCompanyId() == null) {
            return false;
        }

        return companyRepository.findById(userDetails.getCompanyId())
                .filter(company -> PENDING_APPROVAL.equals(company.getApprovalStatus()))
                .filter(company -> company.getReviewRequestedAt() == null)
                .isPresent();
    }

    /**
     * 가입 후 아직 심사를 요청하지 않은 상태인지. 화면 분기에서도 같은 기준을 쓰도록 공개한다.
     * 반려 화면은 별도 흐름이므로 여기서 다루지 않는다.
     */
    public static boolean isDraft(Company company) {
        return PENDING_APPROVAL.equals(company.getApprovalStatus())
                && company.getReviewRequestedAt() == null;
    }
}
