package kr.co.firstdayproject.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.junit.jupiter.api.Test;

/**
 * 로그인 상태로 /auth/login에 접근했을 때 로그인 폼 대신
 * 회원 유형별 첫 화면으로 보내는지 확인한다.
 */
class AuthControllerLoginRedirectTest {

    private final AuthController authController =
            new AuthController(null, null, null, null);

    private CustomUserDetails userDetails(String userType) {
        return new CustomUserDetails(
                User.builder()
                        .userId(1L)
                        .loginId("tester")
                        .passwordHash("encoded")
                        .name("테스터")
                        .email("tester@example.com")
                        .userType(userType)
                        .accountStatus("정상")
                        .build(),
                "PERSONAL",
                true
        );
    }

    @Test
    void showsLoginFormWhenNotAuthenticated() {
        assertThat(authController.login(null)).isEqualTo("auth/login");
    }

    @Test
    void redirectsPersonalMemberToHome() {
        assertThat(authController.login(userDetails("개인")))
                .isEqualTo("redirect:/");
    }

    @Test
    void redirectsCompanyMemberToCorpDashboard() {
        assertThat(authController.login(userDetails("기업")))
                .isEqualTo("redirect:/corp");
    }

    @Test
    void redirectsAdminToAdminHome() {
        assertThat(authController.login(userDetails("관리자")))
                .isEqualTo("redirect:/admin");
    }
}
