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
import kr.co.firstdayproject.repository.member.UserRepository;
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

    @Mock
    private UserRepository userRepository;

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
                properties,
                userRepository
        );

        // 기본값은 "가입되지 않은 이메일". 중복을 확인하는 테스트에서만 따로 바꾼다.
        org.mockito.Mockito.lenient()
                .when(userRepository.existsByEmailIgnoreCase(anyString()))
                .thenReturn(false);

        // 중복 이메일로 발송이 거절되는 테스트는 코드 생성까지 가지 않으므로 lenient로 둔다.
        org.mockito.Mockito.lenient()
                .when(passwordEncoder.encode(anyString()))
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

    /**
     * 예전에는 최종 제출 시점에만 중복을 확인해서, 인증번호를 다 받고 폼을 채운 뒤에야
     * "이미 가입된 이메일"이라는 안내가 나왔다. 발송 자체를 막아 그 시점을 앞당긴다.
     */
    @Test
    void rejectsSignupCodeForAlreadyRegisteredEmail() {
        MockHttpSession session = new MockHttpSession();
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                emailVerificationService.sendVerificationCode(" USER@Example.com ", session)
        )
                .isInstanceOfSatisfying(
                        EmailVerificationException.class,
                        exception -> assertThat(exception.getStatus())
                                .isEqualTo(HttpStatus.CONFLICT)
                )
                .hasMessage("이미 가입된 이메일입니다.");

        // 이미 회원인 사람에게 가입 인증 메일이 나가면 안 된다.
        verify(emailSenderService, org.mockito.Mockito.never())
                .sendVerificationCode(anyString(), anyString(),
                        org.mockito.ArgumentMatchers.anyLong());
    }

    /**
     * 비밀번호 재설정은 반대로 가입된 이메일이어야 정상이다.
     * 가입 쪽 검사가 이 경로까지 막지 않는지 고정한다.
     */
    @Test
    void allowsPasswordResetCodeForRegisteredEmail() {
        MockHttpSession session = new MockHttpSession();

        emailVerificationService.sendPasswordResetCode("user@example.com", session);

        verify(emailSenderService).sendPasswordResetCode(
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(5L)
        );
        // 가입 여부를 아예 묻지 않아야 한다. 물으면 가입된 사용자의 재설정이 막힌다.
        verify(userRepository, org.mockito.Mockito.never())
                .existsByEmailIgnoreCase(anyString());
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
