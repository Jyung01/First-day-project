package kr.co.firstdayproject.dto.admin.member;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.member.User;

public record AdminMemberDetail(
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

    public static AdminMemberDetail from(User user) {
        return new AdminMemberDetail(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAccountStatus(),
                AdminMemberListItem.toStatusCode(user.getAccountStatus()),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getWithdrawnAt()
        );
    }
}
