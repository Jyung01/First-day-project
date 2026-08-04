package kr.co.firstdayproject.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadsCompanyRoleFromUserType() {
        User companyUser = user("company01", "기업", "정상");
        when(userRepository.findByLoginId("company01")).thenReturn(Optional.of(companyUser));

        UserDetails result = userDetailsService.loadUserByUsername("company01");

        assertThat(result.getUsername()).isEqualTo("company01");
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_COMPANY");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) result).getDisplayName()).isEqualTo("테스트 회원");
    }

    @Test
    void disablesSuspendedAccount() {
        User suspendedUser = user("stopped01", "개인", "이용정지");
        when(userRepository.findByLoginId("stopped01")).thenReturn(Optional.of(suspendedUser));

        UserDetails result = userDetailsService.loadUserByUsername("stopped01");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void rejectsUserTypeOutsideDdlCheckConstraint() {
        User invalidUser = user("invalid01", "UNKNOWN", "정상");
        when(userRepository.findByLoginId("invalid01")).thenReturn(Optional.of(invalidUser));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("invalid01"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("지원하지 않는 회원 유형입니다.");
    }

    @Test
    void throwsWhenLoginIdDoesNotExist() {
        when(userRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private User user(String loginId, String userType, String accountStatus) {
        return User.builder()
                .loginId(loginId)
                .passwordHash("$2a$10$encodedPassword")
                .name("테스트 회원")
                .userType(userType)
                .accountStatus(accountStatus)
                .build();
    }
}
