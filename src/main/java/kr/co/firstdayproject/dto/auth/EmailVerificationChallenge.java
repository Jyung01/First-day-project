package kr.co.firstdayproject.dto.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record EmailVerificationChallenge(
        String email,
        String encodedCode,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt,
        int failedAttempts
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public EmailVerificationChallenge withFailedAttempts(int attempts) {
        return new EmailVerificationChallenge(
                email,
                encodedCode,
                expiresAt,
                resendAvailableAt,
                attempts
        );
    }
}
