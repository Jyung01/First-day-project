package kr.co.firstdayproject.service.auth;

import java.time.LocalDateTime;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        int updatedRows = userRepository.updateLastLoginAt(
                userId,
                LocalDateTime.now()
        );
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Failed to update last login time. userId=" + userId
            );
        }
    }
}
