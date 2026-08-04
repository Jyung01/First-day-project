package kr.co.firstdayproject.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginSuccessHandlerTest {

    private final LoginSuccessHandler handler = new LoginSuccessHandler();

    @Test
    void routesPersonalUserToMain() {
        assertThat(handler.resolveDestination(authentication("ROLE_PERSONAL"))).isEqualTo("/");
    }

    @Test
    void routesCompanyUserToCompanyDashboard() {
        assertThat(handler.resolveDestination(authentication("ROLE_COMPANY"))).isEqualTo("/corp");
    }

    @Test
    void routesAdminUserToAdminDashboard() {
        assertThat(handler.resolveDestination(authentication("ROLE_ADMIN"))).isEqualTo("/admin");
    }

    private Authentication authentication(String role) {
        return new UsernamePasswordAuthenticationToken(
                "loginId",
                "password",
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
