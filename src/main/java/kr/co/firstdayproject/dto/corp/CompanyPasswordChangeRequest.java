package kr.co.firstdayproject.dto.corp;

public record CompanyPasswordChangeRequest(
        String currentPassword,
        String newPassword,
        String newPasswordConfirm
) {
}
