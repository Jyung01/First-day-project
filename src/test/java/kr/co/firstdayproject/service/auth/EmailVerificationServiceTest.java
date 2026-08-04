package kr.co.firstdayproject.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.service.common.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties(
                Duration.ofMinutes(5),
                Duration.ofSeconds(60),
                Duration.ofMinutes(30),
                5
        );
        emailVerificationService = new EmailVerificationService(
                emailSenderService,
                passwordEncoder,
                properties
        );

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(invocation ->
                        ("encoded:" + invocation.getArgument(0))
                                .equals(invocation.getArgument(1))
                );
    }

    @Test
    void sendsAndVerifiesCodeInSession() {
        MockHttpSession session = new MockHttpSession();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        emailVerificationService.sendVerificationCode(
                " USER@Example.com ",
                session
        );

        verify(emailSenderService).sendVerificationCode(
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                codeCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(5L)
        );

        emailVerificationService.verifyCode(
                "user@example.com",
                codeCaptor.getValue(),
                session
        );

        Object verifiedState = session.getAttribute(
                EmailVerificationService.VERIFIED_EMAIL_SESSION_KEY
        );
        assertThat(verifiedState).isInstanceOf(VerifiedEmail.class);
        assertThat(((VerifiedEmail) verifiedState).email())
                .isEqualTo("user@example.com");
    }

    @Test
    void rejectsImmediateResend() {
        MockHttpSession session = new MockHttpSession();

        emailVerificationService.sendVerificationCode("user@example.com", session);

        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode("user@example.com", session)
        )
                .isInstanceOfSatisfying(
                        EmailVerificationException.class,
                        exception -> assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                );

        verify(emailSenderService, times(1))
                .sendVerificationCode(anyString(), anyString(),
                        org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsIncorrectCode() {
        MockHttpSession session = new MockHttpSession();
        emailVerificationService.sendVerificationCode("user@example.com", session);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                emailVerificationService.verifyCode(
                        "user@example.com",
                        "000000",
                        session
                )
        )
                .isInstanceOfSatisfying(
                        EmailVerificationException.class,
                        exception -> assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.BAD_REQUEST)
                );
    }
}
