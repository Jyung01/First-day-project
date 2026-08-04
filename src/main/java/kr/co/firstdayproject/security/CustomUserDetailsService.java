package kr.co.firstdayproject.security;

import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String ACTIVE_STATUS = "정상";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        boolean active = ACTIVE_STATUS.equalsIgnoreCase(user.getAccountStatus());

        return new CustomUserDetails(user, resolveRole(user.getUserType()), active);
    }

    private String resolveRole(String userType) {
        return switch (userType) {
            case "개인" -> "PERSONAL";
            case "기업" -> "COMPANY";
            case "관리자" -> "ADMIN";
            default -> throw new UsernameNotFoundException("지원하지 않는 회원 유형입니다.");
        };
    }
}
