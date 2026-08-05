package kr.co.firstdayproject.dto.auth;

public record FindIdResponse(
        boolean found,
        String maskedLoginId,
        String joinedDate,
        String message
) {
}
