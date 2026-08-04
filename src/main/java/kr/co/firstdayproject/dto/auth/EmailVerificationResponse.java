package kr.co.firstdayproject.dto.auth;

public record EmailVerificationResponse(
        boolean success,
        String message
) {
}
