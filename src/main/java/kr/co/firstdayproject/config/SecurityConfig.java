package kr.co.firstdayproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static ServletListenerRegistrationBean<HttpSessionEventPublisher>
    httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(
                new HttpSessionEventPublisher()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            SessionRegistry sessionRegistry,
            @Value("${app.security.csrf.enabled:false}") boolean csrfEnabled
    ) throws Exception {

        /*
         * CSRF 전환 작업 중이라 기본값은 비활성이다.
         * 로컬에서 app.security.csrf.enabled=true로 켜고 화면을 눌러보면서
         * CsrfAuditFilter가 남기는 MISSING 로그로 누락된 호출부를 찾는다.
         *
         * spa()는 XSRF-TOKEN 쿠키 발급과 X-XSRF-TOKEN 헤더 검증을 함께 설정해 준다.
         * 로그인·로그아웃 직후 토큰을 새로 내려주는 처리까지 포함되어 있어,
         * 쿠키 방식에서 흔히 겪는 "인증 후 토큰이 갱신되지 않는" 문제를 피할 수 있다.
         * 클라이언트 쪽은 static/js/common/csrf.js가 fetch를 감싸 헤더를 붙인다.
         */
        if (csrfEnabled) {
            http.csrf(csrf -> csrf.spa())
                    /*
                     * 조사용 필터는 CSRF를 켤 때만, 그리고 반드시 CsrfFilter 바로 앞에 넣는다.
                     * 서블릿 필터로 자동 등록하면 CharacterEncodingFilter보다 먼저 돌면서
                     * 요청 본문을 기본 인코딩으로 파싱해버려 한글 폼 입력이 깨진다.
                     */
                    .addFilterBefore(new CsrfAuditFilter(), CsrfFilter.class);
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        http
                .authorizeHttpRequests(auth -> auth
                        // TODO 권한 정책 확정 후 아래 규칙부터 순서대로 활성화
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/corp/**").hasRole("COMPANY")
                        .requestMatchers("/my/**").hasRole("PERSONAL")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/auth/login?accountStatus=restricted")
                )
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
