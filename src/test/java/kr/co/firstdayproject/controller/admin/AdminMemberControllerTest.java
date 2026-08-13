package kr.co.firstdayproject.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import kr.co.firstdayproject.dto.admin.member.AdminMemberDetail;
import kr.co.firstdayproject.service.admin.AdminMemberService;
import kr.co.firstdayproject.service.admin.MemberSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;

class AdminMemberControllerTest {

    @Test
    void expiresSessionsAfterMemberWasSuspended() {
        AdminMemberService memberService = mock(AdminMemberService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminMemberController controller = new AdminMemberController(
                memberService,
                sessionService
        );
        AdminMemberDetail suspendedMember = detail("이용정지", "SUSPENDED");
        when(memberService.suspendMember(12L)).thenReturn(suspendedMember);

        ResponseEntity<AdminMemberDetail> response = controller.suspend(12L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        InOrder order = inOrder(memberService, sessionService);
        order.verify(memberService).suspendMember(12L);
        order.verify(sessionService).expireAllSessions(12L);
    }

    @Test
    void doesNotExpireSessionsWhenSuspensionWasRejected() {
        AdminMemberService memberService = mock(AdminMemberService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminMemberController controller = new AdminMemberController(
                memberService,
                sessionService
        );
        when(memberService.suspendMember(12L))
                .thenThrow(new IllegalStateException("invalid status"));

        ResponseEntity<AdminMemberDetail> response = controller.suspend(12L);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        verifyNoInteractions(sessionService);
    }

    private AdminMemberDetail detail(String accountStatus, String statusCode) {
        return new AdminMemberDetail(
                12L,
                "member12",
                "회원",
                "member12@example.com",
                "010-1234-5678",
                accountStatus,
                statusCode,
                null,
                null,
                null
        );
    }
}
