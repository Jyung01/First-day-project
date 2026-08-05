package kr.co.firstdayproject.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetCodeRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        String loginId,
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일을 입력해주세요.")
        String email
) {
}
