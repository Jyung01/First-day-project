package kr.co.firstdayproject.controller.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.company.CompanySearchDTO;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.banner.BannerService;
import kr.co.firstdayproject.service.company.CompanyService;
import kr.co.firstdayproject.service.job.SavedJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import kr.co.firstdayproject.util.PageHandler;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private BannerService bannerService;

    @Mock
    private SavedJobService savedJobService;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private CompanyController companyController;

    @Test
    void keepsZeroOffsetWhenCompanySearchHasNoResults() {
        CompanySearchDTO search = new CompanySearchDTO();
        ExtendedModelMap model = new ExtendedModelMap();
        when(companyService.getCompanyCount(search)).thenReturn(0);
        when(companyService.getCompanyList(search)).thenReturn(List.of());

        String view = companyController.list(-3, search, model, null);

        assertThat(view).isEqualTo("company/list");
        assertThat(search.getOffset()).isZero();
        assertThat(model.get("companyCount")).isEqualTo(0);
        assertThat(((PageHandler) model.get("ph")).getCurrentPage()).isEqualTo(1);
    }

    @Test
    void clampsCompanyPageToLastAvailablePage() {
        CompanySearchDTO search = new CompanySearchDTO();
        ExtendedModelMap model = new ExtendedModelMap();
        when(companyService.getCompanyCount(search)).thenReturn(13);
        when(companyService.getCompanyList(search)).thenReturn(List.of());

        companyController.list(99, search, model, null);

        assertThat(search.getOffset()).isEqualTo(12);
        assertThat(((PageHandler) model.get("ph")).getCurrentPage()).isEqualTo(3);
    }

    @Test
    void returnsUnauthorizedWhenAnonymousUserTogglesWish() {
        ResponseEntity<Map<String, Object>> response =
                companyController.toggleWish(10L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "로그인이 필요합니다.");
    }

    @Test
    void returnsUpdatedWishStateWhenAuthenticatedUserTogglesWish() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUserId()).thenReturn(1L);
        when(companyService.toggleWish(1L, 10L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response =
                companyController.toggleWish(10L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("success", true)
                .containsEntry("wished", true);
    }
}
