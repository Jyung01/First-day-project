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
                /*
                 * 역할 기반 접근 제어는 아래 세 접두사에만 걸려 있다.
                 * 그 밖의 경로(/job, /company, /cs, /reports, /salary, /api/** 등)는
                 * permitAll이며, 로그인·본인 확인은 각 컨트롤러와 서비스가 직접 한다.
                 * 예) SavedJobService.getPersonalUserId, QnaController의 @AuthenticationPrincipal
                 *
                 * 따라서 이 영역에 쓰기 엔드포인트를 새로 추가할 때는
                 * 여기서 막아주지 않는다는 점을 전제로 방어 코드를 함께 넣어야 한다.
                 *
                 * /actuator/**는 여기서 막지 말 것.
                 * EC2의 deploy.sh가 배포 직후 127.0.0.1:8080/actuator/health로 헬스체크를 하고,
                 * 실패하면 이전 버전으로 롤백한다. 앱에서 차단하면 정상 배포까지 전부 롤백된다.
                 * 외부 노출 차단은 Apache에서 처리한다(docs/operations/domain-https-handoff.md 5-1).
                 */
                .authorizeHttpRequests(auth -> auth
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
