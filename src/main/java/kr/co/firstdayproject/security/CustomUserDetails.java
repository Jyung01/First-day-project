package kr.co.firstdayproject.security;

import java.util.List;
import kr.co.firstdayproject.entity.member.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final Long companyId;
    private final String displayName;
    private final String userType;

    public CustomUserDetails(User user, String role, boolean active) {
        super(
                user.getLoginId(),
                user.getPasswordHash(),
                active,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        this.userId = user.getUserId();
        this.companyId = user.getCompanyId();
        this.displayName = user.getName();
        this.userType = user.getUserType();
    }
}
