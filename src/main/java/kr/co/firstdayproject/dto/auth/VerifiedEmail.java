package kr.co.firstdayproject.dto.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record VerifiedEmail(
        String email,
        LocalDateTime verifiedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
