package kr.co.firstdayproject.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

class MemberSessionServiceTest {

    @Test
    void expiresEverySessionBelongingToMember() {
        SessionRegistry sessionRegistry = mock(SessionRegistry.class);
        SessionInformation firstSession = mock(SessionInformation.class);
        SessionInformation secondSession = mock(SessionInformation.class);
        CustomUserDetails principal = principal(12L, "member12");

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false))
                .thenReturn(List.of(firstSession, secondSession));
        MemberSessionService service = new MemberSessionService(sessionRegistry);

        int expiredCount = service.expireAllSessions(12L);

        assertThat(expiredCount).isEqualTo(2);
        verify(firstSession).expireNow();
        verify(secondSession).expireNow();
    }

    private CustomUserDetails principal(Long userId, String loginId) {
        User user = User.builder()
                .userId(userId)
                .loginId(loginId)
                .passwordHash("encoded-password")
                .name("회원")
                .userType("개인")
                .build();
        return new CustomUserDetails(user, "PERSONAL", true);
    }
}
