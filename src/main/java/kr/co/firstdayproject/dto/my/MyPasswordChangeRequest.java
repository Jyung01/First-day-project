package kr.co.firstdayproject.dto.my;

public record MyPasswordChangeRequest(
        String currentPassword,
        String newPassword,
        String newPasswordConfirm
) {
}
