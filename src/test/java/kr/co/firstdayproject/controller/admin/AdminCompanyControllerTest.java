package kr.co.firstdayproject.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import kr.co.firstdayproject.dto.admin.company.AdminCompanyDetail;
import kr.co.firstdayproject.dto.admin.company.AdminCompanyReviewRequest;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.admin.AdminCompanyReviewException;
import kr.co.firstdayproject.service.admin.AdminCompanyService;
import kr.co.firstdayproject.service.admin.MemberSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;

class AdminCompanyControllerTest {

    @Test
    void returnsCompanyDetail() {
        AdminCompanyService service = mock(AdminCompanyService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminCompanyController controller = new AdminCompanyController(
                service,
                sessionService
        );
        AdminCompanyDetail detail = detail();
        when(service.getCompanyDetail(12L)).thenReturn(detail);

        ResponseEntity<AdminCompanyDetail> response = controller.detail(12L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void returnsNotFoundForMissingCompany() {
        AdminCompanyService service = mock(AdminCompanyService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminCompanyController controller = new AdminCompanyController(
                service,
                sessionService
        );
        when(service.getCompanyDetail(99L))
                .thenThrow(new IllegalArgumentException("missing"));

        ResponseEntity<AdminCompanyDetail> response = controller.detail(99L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void expiresCompanySessionsAfterSuspensionSucceeded() {
        AdminCompanyService service = mock(AdminCompanyService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminCompanyController controller = new AdminCompanyController(
                service,
                sessionService
        );
        AdminCompanyDetail suspended = detailWithStatus(
                "SUSPENDED",
                "이용정지"
        );
        when(service.suspendCompany(12L)).thenReturn(suspended);

        ResponseEntity<AdminCompanyDetail> response = controller.suspend(12L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        InOrder order = inOrder(service, sessionService);
        order.verify(service).suspendCompany(12L);
        order.verify(sessionService).expireAllCompanySessions(12L);
    }

    @Test
    void doesNotExpireSessionsWhenSuspensionWasRejected() {
        AdminCompanyService service = mock(AdminCompanyService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminCompanyController controller = new AdminCompanyController(
                service,
                sessionService
        );
        when(service.suspendCompany(12L))
                .thenThrow(new IllegalStateException("invalid status"));

        ResponseEntity<AdminCompanyDetail> response = controller.suspend(12L);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        verifyNoInteractions(sessionService);
    }

    @Test
    void returnsBadRequestForInvalidRejectionRequest() {
        AdminCompanyService service = mock(AdminCompanyService.class);
        MemberSessionService sessionService = mock(MemberSessionService.class);
        AdminCompanyController controller = new AdminCompanyController(
                service,
                sessionService
        );
        CustomUserDetails admin = mock(CustomUserDetails.class);
        AdminCompanyReviewRequest request = new AdminCompanyReviewRequest(
                "UNKNOWN",
                "안내"
        );
        when(admin.getUserId()).thenReturn(99L);
        when(service.rejectCompany(99L, 12L, request))
                .thenThrow(new AdminCompanyReviewException("invalid"));

        ResponseEntity<AdminCompanyDetail> response = controller.reject(
                admin,
                12L,
                request
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    private AdminCompanyDetail detail() {
        return detailWithStatus("PENDING", "승인 대기");
    }

    private AdminCompanyDetail detailWithStatus(
            String statusCode,
            String statusLabel
    ) {
        return new AdminCompanyDetail(
                12L,
                "C-0012",
                "코드웨이브",
                "123-45-67890",
                "김대표",
                null,
                "IT 서비스업",
                "중소기업",
                "서울시 강남구",
                "https://example.com",
                "한 줄 소개",
                "기업 소개",
                "유연근무제",
                "김담당",
                "manager@example.com",
                "010-1234-5678",
                null,
                null,
                null,
                "NEW",
                "신규 심사",
                statusCode,
                statusLabel,
                null,
                null,
                null
        );
    }
}
