package kr.co.firstdayproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF 토큰 없이 들어오는 상태변경 요청을 찾아내기 위한 조사용 필터.
 *
 * <p>아무 요청도 차단하지 않는다. 403으로 거부된(또는 거부됐을) 요청을 브라우저 네트워크 탭에서
 * 하나씩 찾는 대신, grep으로 모아볼 수 있게 로그로 남기는 것이 목적이다.
 *
 * <p>전환이 끝나 CSRF가 항상 켜진 상태가 되면 이 필터는 역할이 끝나므로 삭제한다.
 *
 * <p><b>등록 위치가 중요하다.</b> {@code SecurityConfig}가 CSRF를 켤 때만
 * {@code CsrfFilter} 바로 앞에 넣는다({@code addFilterBefore}). 이 자리여야 두 조건을 동시에 만족한다.
 *
 * <ul>
 *   <li>{@code CsrfFilter}가 403으로 체인을 끊기 <b>전</b>이라 정작 기록하고 싶은 요청을 볼 수 있다.</li>
 *   <li>{@code CharacterEncodingFilter}가 이미 지나간 <b>뒤</b>다. 이게 어긋나면
 *       아래 {@code getParameter} 호출이 요청 본문을 기본 인코딩(ISO-8859-1)으로 파싱해 캐시해버려,
 *       한글 폼 입력이 전부 깨진다.</li>
 * </ul>
 *
 * <p>서블릿 필터로 자동 등록되지 않도록 {@code @Component}를 붙이지 않는다.
 * 자동 등록되면 체인 맨 앞에서 한 번 더 돌아 위의 인코딩 문제가 그대로 재현된다.
 */
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isMutating(request) && !hasToken(request)) {
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
