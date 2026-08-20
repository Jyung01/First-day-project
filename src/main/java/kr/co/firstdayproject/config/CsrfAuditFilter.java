package kr.co.firstdayproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF 토큰 없이 들어오는 상태변경 요청을 찾아내기 위한 조사용 필터.
 *
 * <p>아무 요청도 차단하지 않는다. 403으로 거부된(또는 거부됐을) 요청을 브라우저 네트워크 탭에서
 * 하나씩 찾는 대신, grep으로 모아볼 수 있게 로그로 남기는 것이 목적이다.
 *
 * <p>{@code app.security.csrf.enabled=true}일 때만 기록한다. CSRF가 꺼져 있으면 토큰 자체가
 * 발급되지 않아 모든 상태변경 요청이 예외 없이 걸리고, 그러면 신호가 아니라 소음이 된다.
 *
 * <p>전환이 끝나 CSRF가 항상 켜진 상태가 되면 이 필터는 역할이 끝나므로 삭제한다.
 *
 * <p><b>순서가 중요하다.</b> Spring Security 필터 체인은 음수 순서로 앞쪽에서 돌고,
 * 토큰이 없으면 {@code CsrfFilter}가 그 자리에서 403을 내고 체인을 끊는다.
 * 기본 순서(가장 낮은 우선순위)로 두면 정작 기록하고 싶은 요청이 여기까지 오지 못한다.
 * 그래서 Security보다 앞서 실행되도록 최우선 순위를 준다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CsrfAuditFilter extends OncePerRequestFilter {

    /**
     * 클래스명 대신 고정 이름을 쓴다. 로그 레벨 조정과 grep이 쉬워진다.
     */
    private static final Logger AUDIT = LoggerFactory.getLogger("CSRF_AUDIT");

    /**
     * CSRF 검증 대상에서 제외되는 메서드. Spring Security의 기본 정책과 같다.
     */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /**
     * CookieCsrfTokenRepository를 쓸 때 클라이언트가 보내는 헤더 이름.
     */
    private static final String TOKEN_HEADER = "X-XSRF-TOKEN";

    /**
     * Thymeleaf 폼이 자동으로 넣어주는 hidden 필드 이름.
     */
    private static final String TOKEN_PARAMETER = "_csrf";

    /**
     * SecurityConfig의 CSRF 활성화 여부와 같은 속성을 본다. 둘이 어긋나지 않도록 값을 공유한다.
     */
    private final boolean csrfEnabled;

    public CsrfAuditFilter(@Value("${app.security.csrf.enabled:false}") boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (csrfEnabled && isMutating(request) && !hasToken(request)) {
            AUDIT.warn("MISSING {} {}", request.getMethod(), path(request));
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMutating(HttpServletRequest request) {
        return !SAFE_METHODS.contains(request.getMethod());
    }

    /**
     * 헤더와 파라미터를 모두 본다. AJAX는 헤더로, 폼은 hidden 필드로 토큰을 보내기 때문이다.
     *
     * <p>주의: multipart 요청에서 {@code getParameter}를 호출하면 요청 본문이 읽히면서
     * 뒤쪽 컨트롤러의 파일 파싱이 깨질 수 있다. multipart는 헤더만 확인한다.
     */
    private boolean hasToken(HttpServletRequest request) {
        if (request.getHeader(TOKEN_HEADER) != null) {
            return true;
        }
        if (isMultipart(request)) {
            return false;
        }
        return request.getParameter(TOKEN_PARAMETER) != null;
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * 로그를 URL별로 집계하기 쉽도록 쿼리스트링은 빼고 경로만 남긴다.
     */
    private String path(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
