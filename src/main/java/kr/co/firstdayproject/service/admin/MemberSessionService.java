package kr.co.firstdayproject.service.admin;

import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberSessionService {

    private final SessionRegistry sessionRegistry;

    public int expireAllSessions(Long memberId) {
        int expiredSessionCount = 0;

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!(principal instanceof CustomUserDetails userDetails)
                    || !memberId.equals(userDetails.getUserId())) {
                continue;
            }

            for (var session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
                expiredSessionCount += 1;
            }
        }

        return expiredSessionCount;
    }
}
