package kr.co.firstdayproject.dto.auth;

public record LoginIdAvailabilityResponse(
        boolean available,
        String message
) {
}
