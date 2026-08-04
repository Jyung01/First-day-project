package kr.co.firstdayproject.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$",
                message = "도메인을 포함한 올바른 이메일을 입력해주세요."
        )
        @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
        String email,

        @NotBlank(message = "인증번호를 입력해주세요.")
        @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해주세요.")
        String code
) {
}
