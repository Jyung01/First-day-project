package kr.co.firstdayproject.service.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import kr.co.firstdayproject.repository.member.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAuditServiceTest {

    private UserRepository userRepository;
    private LoginAuditService loginAuditService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        loginAuditService = new LoginAuditService(userRepository);
    }

    @Test
    void updatesLastLoginTimeForAuthenticatedUser() {
        when(userRepository.updateLastLoginAt(any(), any())).thenReturn(1);

        loginAuditService.recordSuccessfulLogin(100L);

        verify(userRepository).updateLastLoginAt(
                org.mockito.ArgumentMatchers.eq(100L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void failsWhenUserWasNotUpdated() {
        when(userRepository.updateLastLoginAt(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> loginAuditService.recordSuccessfulLogin(100L))
                .isInstanceOf(IllegalStateException.class);
    }
}
