package kr.co.firstdayproject.dto.auth;

public record BusinessNumberAvailabilityResponse(
        boolean available,
        String message
) {
}
