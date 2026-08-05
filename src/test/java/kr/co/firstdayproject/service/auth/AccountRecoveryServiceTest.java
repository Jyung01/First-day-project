package kr.co.firstdayproject.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.FindIdResponse;
import kr.co.firstdayproject.dto.auth.PasswordResetRequest;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountRecoveryServiceTest {

    private UserRepository userRepository;
    private EmailVerificationService emailVerificationService;
    private PasswordEncoder passwordEncoder;
    private AccountRecoveryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailVerificationService = mock(EmailVerificationService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AccountRecoveryService(
                userRepository,
                emailVerificationService,
                new EmailVerificationProperties(
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(60),
                        Duration.ofMinutes(30),
                        5
                ),
                passwordEncoder
        );
    }

    @Test
    void findsAndMasksLoginId() {
        when(userRepository.findByNameAndEmailIgnoreCase(
                "홍길동",
                "user@example.com"
        )).thenReturn(Optional.of(User.builder()
                .loginId("personal01")
                .accountStatus("정상")
                .createdAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build()));

        FindIdResponse result = service.findId(
                " 홍길동 ",
                " USER@Example.com "
        );

        assertThat(result.found()).isTrue();
        assertThat(result.maskedLoginId()).isEqualTo("per*******");
        assertThat(result.joinedDate()).isEqualTo("2026.07.20");
    }

    @Test
    void doesNotFindWithdrawnAccount() {
        when(userRepository.findByNameAndEmailIgnoreCase(
                "홍길동",
                "user@example.com"
        )).thenReturn(Optional.of(User.builder()
                .loginId("personal01")
                .accountStatus("탈퇴")
                .build()));

        FindIdResponse result = service.findId("홍길동", "user@example.com");

        assertThat(result.found()).isFalse();
    }

    @Test
    void sendsResetCodeOnlyForMatchingAccount() {
        User user = recoverableUser();
        MockHttpSession session = new MockHttpSession();
        when(userRepository.findByLoginIdAndEmailIgnoreCase(
                "personal01",
                "user@example.com"
        )).thenReturn(Optional.of(user));

        service.sendPasswordResetCode(
                "personal01",
                "USER@example.com",
                session
        );

        verify(emailVerificationService).sendPasswordResetCode(
                "USER@example.com",
                session
        );
    }

    @Test
    void changesPasswordAfterVerifiedEmail() {
        User user = recoverableUser();
        when(userRepository.findByLoginIdAndEmailIgnoreCase(
                "personal01",
                "user@example.com"
        )).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPassword1!", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword1!"))
                .thenReturn("new-hash");
        PasswordResetRequest request = new PasswordResetRequest(
                "personal01",
                "user@example.com",
                "NewPassword1!",
                "NewPassword1!"
        );

        service.resetPassword(
                request,
                new VerifiedEmail("user@example.com", LocalDateTime.now())
        );

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsResetWithoutVerifiedEmail() {
        when(userRepository.findByLoginIdAndEmailIgnoreCase(
                "personal01",
                "user@example.com"
        )).thenReturn(Optional.of(recoverableUser()));
        PasswordResetRequest request = new PasswordResetRequest(
                "personal01",
                "user@example.com",
                "NewPassword1!",
                "NewPassword1!"
        );

        assertThatThrownBy(() -> service.resetPassword(request, null))
                .isInstanceOf(AccountRecoveryException.class)
                .hasMessageContaining("이메일 인증");
    }

    private User recoverableUser() {
        return User.builder()
                .loginId("personal01")
                .email("user@example.com")
                .passwordHash("old-hash")
                .accountStatus("정상")
                .build();
    }
}
