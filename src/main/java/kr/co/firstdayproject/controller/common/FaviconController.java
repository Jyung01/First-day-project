package kr.co.firstdayproject.controller.common;

import java.net.URI;
import java.time.Duration;
import kr.co.firstdayproject.service.admin.config.AdminSiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 브라우저가 자동으로 요청하는 /favicon.ico를 관리자에 등록된 파비콘으로 연결한다.
 *
 * 화면 템플릿마다 head를 따로 관리하는 구조라 링크 태그를 각 페이지에 넣으면
 * 새 페이지가 추가될 때 또 누락된다(실제로 그렇게 빠져 있었다).
 * 브라우저는 아이콘 링크가 없으면 항상 이 경로를 찾으므로,
 * 여기서 한 번만 처리하면 페이지 수와 무관하게 전 화면에 적용된다.
 */
@RestController
@RequiredArgsConstructor
public class FaviconController {

    /**
     * 리다이렉트 응답을 캐시하는 시간.
     * 브라우저의 파비콘 캐시는 매우 공격적이라, 관리자가 이미지를 교체했을 때
     * 반영이 지나치게 늦지 않도록 짧게 잡는다.
     */
    private static final Duration REDIRECT_CACHE_TTL = Duration.ofHours(1);

    private final AdminSiteSettingService adminSiteSettingService;

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        String faviconUrl = adminSiteSettingService.getSiteSettingView().faviconUrl();

        // 아직 등록 전이면 404를 그대로 돌려준다. 빈 값으로 리다이렉트하면
        // 브라우저가 같은 경로를 다시 요청해 무한 루프가 된다.
        if (faviconUrl == null || faviconUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(faviconUrl))
                .cacheControl(CacheControl.maxAge(REDIRECT_CACHE_TTL).cachePublic())
                .build();
    }
}
