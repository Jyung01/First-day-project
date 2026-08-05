package kr.co.firstdayproject.service.auth;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;
import kr.co.firstdayproject.config.properties.EmailVerificationProperties;
import kr.co.firstdayproject.dto.auth.FindIdResponse;
import kr.co.firstdayproject.dto.auth.PasswordResetRequest;
import kr.co.firstdayproject.dto.auth.VerifiedEmail;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRecoveryService {

    private static final String WITHDRAWN_STATUS = "탈퇴";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s])\\S{8,64}$"
    );
    private static final DateTimeFormatter JOINED_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final PasswordEncoder passwordEncoder;

    public FindIdResponse findId(String name, String email) {
        return userRepository.findByNameAndEmailIgnoreCase(
                        name.trim(),
                        normalizeEmail(email)
                )
                .filter(this::isRecoverable)
                .map(user -> new FindIdResponse(
                        true,
                        maskLoginId(user.getLoginId()),
                        user.getCreatedAt() == null
                                ? "-"
                                : user.getCreatedAt().format(JOINED_DATE_FORMAT),
                        "아이디를 찾았습니다."
                ))
                .orElseGet(() -> new FindIdResponse(
                        false,
                        null,
                        null,
                        "입력한 정보와 일치하는 계정을 찾을 수 없습니다."
                ));
    }

    public void sendPasswordResetCode(
            String loginId,
            String email,
            HttpSession session
    ) {
        findRecoverableUser(loginId, email);
        emailVerificationService.sendPasswordResetCode(email, session);
    }

    @Transactional
    public void resetPassword(
            PasswordResetRequest request,
            VerifiedEmail verifiedEmail
    ) {
        User user = findRecoverableUser(request.loginId(), request.email());
        String normalizedEmail = normalizeEmail(request.email());
        LocalDateTime validAfter = LocalDateTime.now()
                .minus(emailVerificationProperties.verifiedValidity());

        if (verifiedEmail == null
                || !normalizedEmail.equals(verifiedEmail.email())
                || verifiedEmail.verifiedAt().isBefore(validAfter)) {
            throw new AccountRecoveryException(
                    HttpStatus.BAD_REQUEST,
                    "이메일 인증이 만료되었거나 완료되지 않았습니다."
            );
        }
        if (!PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw new AccountRecoveryException(
                    HttpStatus.BAD_REQUEST,
                    "비밀번호는 8~64자의 영문, 숫자, 특수문자를 포함해야 합니다."
            );
        }
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new AccountRecoveryException(
                    HttpStatus.BAD_REQUEST,
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AccountRecoveryException(
                    HttpStatus.BAD_REQUEST,
                    "기존 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(now);
        user.setUpdatedAt(now);
    }

    private User findRecoverableUser(String loginId, String email) {
        return userRepository.findByLoginIdAndEmailIgnoreCase(
                        loginId.trim(),
                        normalizeEmail(email)
                )
                .filter(this::isRecoverable)
                .orElseThrow(() -> new AccountRecoveryException(
                        HttpStatus.BAD_REQUEST,
                        "아이디와 이메일이 일치하는 계정을 찾을 수 없습니다."
                ));
    }

    private boolean isRecoverable(User user) {
        return !WITHDRAWN_STATUS.equals(user.getAccountStatus());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskLoginId(String loginId) {
        int visibleLength = Math.min(3, Math.max(1, loginId.length() / 2));
        return loginId.substring(0, visibleLength)
                + "*".repeat(loginId.length() - visibleLength);
    }
}
