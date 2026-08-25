package kr.co.firstdayproject.controller.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import kr.co.firstdayproject.dto.admin.config.SiteSettingView;
import kr.co.firstdayproject.service.site.SiteSettingQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FaviconControllerTest {

    private SiteSettingQueryService siteSettingQueryService;
    private FaviconController faviconController;

    @BeforeEach
    void setUp() {
        siteSettingQueryService = mock(SiteSettingQueryService.class);
        faviconController = new FaviconController(siteSettingQueryService);
    }

    @Test
    void redirectsToRegisteredFaviconUrl() {
        givenFaviconUrl("https://cdn.example.net/site/brand-images/favicon.png");

        ResponseEntity<Void> response = faviconController.favicon();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://cdn.example.net/site/brand-images/favicon.png");
    }

    @Test
    void limitsRedirectCachingSoReplacedFaviconIsPickedUp() {
        givenFaviconUrl("https://cdn.example.net/site/brand-images/favicon.png");

        ResponseEntity<Void> response = faviconController.favicon();

        assertThat(response.getHeaders().getCacheControl()).contains("max-age=3600");
    }

    /**
     * 빈 값으로 리다이렉트하면 브라우저가 /favicon.ico를 다시 요청해 무한 루프가 된다.
     * 미등록 상태에서는 반드시 404여야 한다.
     */
    @Test
    void returnsNotFoundWhenFaviconIsNotRegistered() {
        givenFaviconUrl(null);
        assertThat(faviconController.favicon().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        givenFaviconUrl("   ");
        assertThat(faviconController.favicon().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void givenFaviconUrl(String faviconUrl) {
        when(siteSettingQueryService.getSiteSettingView()).thenReturn(
                new SiteSettingView(
                        "첫출근", "help@firstwork.co.kr", "02-1234-5678",
                        "평일 09:00 ~ 18:00", "설레는 첫 출근을 함께 준비합니다.",
                        "첫출근", "1112233333", "서울시", "© 2026 FirstWork Inc.",
                        "https://cdn.example.net/header.png",
                        "https://cdn.example.net/footer.png",
                        faviconUrl
                )
        );
    }
}
