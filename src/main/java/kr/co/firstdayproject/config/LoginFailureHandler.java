package kr.co.firstdayproject.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String SUSPENDED_STATUS = "이용정지";
    private static final String RESTRICTED_LOGIN_URL =
            "/auth/login?accountStatus=restricted";
    private static final String DEFAULT_FAILURE_URL = "/auth/login?error";

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String loginId = request.getParameter("username");

        boolean suspended = exception instanceof DisabledException
                && loginId != null
                && userRepository.findByLoginId(loginId.trim())
                .map(user -> SUSPENDED_STATUS.equals(user.getAccountStatus()))
                .orElse(false);

        String destination = suspended
                ? RESTRICTED_LOGIN_URL
                : DEFAULT_FAILURE_URL;
        response.sendRedirect(request.getContextPath() + destination);
    }
}
