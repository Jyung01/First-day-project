package kr.co.firstdayproject.dto.admin.member;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.member.User;

public record AdminMemberListItem(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String accountStatus,
        String statusCode,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        LocalDateTime withdrawnAt
) {

    public static AdminMemberListItem from(User user) {
        return new AdminMemberListItem(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAccountStatus(),
                toStatusCode(user.getAccountStatus()),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getWithdrawnAt()
        );
    }

    static String toStatusCode(String accountStatus) {
        return switch (accountStatus) {
            case "이용정지" -> "SUSPENDED";
            case "탈퇴" -> "WITHDRAWN";
            default -> "ACTIVE";
        };
    }
}
