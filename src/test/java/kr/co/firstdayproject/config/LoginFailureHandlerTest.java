package kr.co.firstdayproject.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

class LoginFailureHandlerTest {

    private UserRepository userRepository;
    private LoginFailureHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        handler = new LoginFailureHandler(userRepository);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void redirectsSuspendedAccountToRestrictedModal() throws Exception {
        when(request.getParameter("username")).thenReturn("stopped01");
        when(userRepository.findByLoginId("stopped01")).thenReturn(Optional.of(
                User.builder().accountStatus("이용정지").build()
        ));

        handler.onAuthenticationFailure(
                request,
                response,
                new DisabledException("disabled")
        );

        verify(response).sendRedirect("/auth/login?accountStatus=restricted");
    }

    @Test
    void keepsNormalFailureForWrongPassword() throws Exception {
        when(request.getParameter("username")).thenReturn("normal01");

        handler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("bad credentials")
        );

        verify(response).sendRedirect("/auth/login?error");
    }
}
