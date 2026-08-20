package kr.co.firstdayproject.service.auth;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.EmailVerificationChallenge;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.service.common.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    public static final String VERIFIED_EMAIL_SESSION_KEY = "verifiedSignupEmail";

    private static final String CHALLENGE_SESSION_KEY = "emailVerificationChallenge";
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailSenderService emailSenderService;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationProperties properties;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailSenderService emailSenderService,
            PasswordEncoder passwordEncoder,
            EmailVerificationProperties properties,
            UserRepository userRepository
    ) {
        this.emailSenderService = emailSenderService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.userRepository = userRepository;
    }

    /**
     * 회원가입용 인증번호 발송.
     *
     * <p>이미 가입된 이메일이면 발송하지 않고 거절한다. 예전에는 최종 제출 시점에만 검사해서,
     * 사용자가 인증번호를 받고 폼을 다 채운 뒤에야 "이미 가입된 이메일"이라는 안내를 받았다.
     * 이미 회원인 사람에게 가입 인증 메일이 나가는 문제도 있었다.
     *
     * <p>비밀번호 재설정({@link #sendPasswordResetCode})은 반대로 가입된 이메일이어야 하므로
     * 이 검사를 적용하지 않는다.
     */
    public void sendVerificationCode(String email, HttpSession session) {
        if (userRepository.existsByEmailIgnoreCase(normalizeEmail(email))) {
            throw new EmailVerificationException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        sendVerificationCode(email, session, false);
    }

    public void sendPasswordResetCode(String email, HttpSession session) {
        sendVerificationCode(email, session, true);
    }

    private void sendVerificationCode(
            String email,
            HttpSession session,
            boolean passwordReset
    ) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        session.removeAttribute(VERIFIED_EMAIL_SESSION_KEY);

        Object currentState = session.getAttribute(CHALLENGE_SESSION_KEY);
        if (currentState instanceof EmailVerificationChallenge current
                && now.isBefore(current.resendAvailableAt())) {
            long remainingSeconds = Math.max(
                    1,
                    java.time.Duration.between(now, current.resendAvailableAt()).toSeconds()
            );
            throw new EmailVerificationException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    remainingSeconds + "초 후에 인증번호를 다시 요청해주세요."
            );
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        try {
            long expirationMinutes = Math.max(
                    1,
                    properties.expiration().toMinutes()
            );
            if (passwordReset) {
                emailSenderService.sendPasswordResetCode(
                        normalizedEmail,
                        code,
                        expirationMinutes
                );
            } else {
                emailSenderService.sendVerificationCode(
                        normalizedEmail,
                        code,
                        expirationMinutes
                );
            }
        } catch (MailException exception) {
            log.error("이메일 인증번호 발송에 실패했습니다.", exception);
            throw new EmailVerificationException(
                    HttpStatus.BAD_GATEWAY,
                    "인증번호를 발송하지 못했습니다. 잠시 후 다시 시도해주세요."
            );
        }

        session.setAttribute(
                CHALLENGE_SESSION_KEY,
                new EmailVerificationChallenge(
                        normalizedEmail,
                        passwordEncoder.encode(code),
                        now.plus(properties.expiration()),
                        now.plus(properties.resendCooldown()),
                        0
                )
        );
    }

    public void verifyCode(String email, String code, HttpSession session) {
        Object state = session.getAttribute(CHALLENGE_SESSION_KEY);
        if (!(state instanceof EmailVerificationChallenge challenge)) {
            throw new EmailVerificationException(
                    HttpStatus.BAD_REQUEST,
                    "먼저 인증번호를 요청해주세요."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(challenge.expiresAt())) {
            session.removeAttribute(CHALLENGE_SESSION_KEY);
            throw new EmailVerificationException(
                    HttpStatus.GONE,
                    "인증번호가 만료되었습니다. 다시 요청해주세요."
            );
        }

        String normalizedEmail = normalizeEmail(email);
        if (!challenge.email().equals(normalizedEmail)) {
            throw new EmailVerificationException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호를 요청한 이메일과 일치하지 않습니다."
            );
        }

        if (challenge.failedAttempts() >= properties.maxAttempts()) {
            throw new EmailVerificationException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."
            );
        }

        if (!passwordEncoder.matches(code, challenge.encodedCode())) {
            int failedAttempts = challenge.failedAttempts() + 1;
            session.setAttribute(
                    CHALLENGE_SESSION_KEY,
                    challenge.withFailedAttempts(failedAttempts)
            );

            if (failedAttempts >= properties.maxAttempts()) {
                throw new EmailVerificationException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."
                );
            }

            throw new EmailVerificationException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호가 일치하지 않습니다."
            );
        }

        session.removeAttribute(CHALLENGE_SESSION_KEY);
        session.setAttribute(
                VERIFIED_EMAIL_SESSION_KEY,
                new VerifiedEmail(normalizedEmail, now)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
