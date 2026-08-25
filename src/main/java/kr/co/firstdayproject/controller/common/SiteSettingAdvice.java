package kr.co.firstdayproject.controller.common;

import kr.co.firstdayproject.dto.admin.config.SiteSettingView;
import kr.co.firstdayproject.service.site.SiteSettingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 공통 헤더·푸터가 쓰는 사이트 설정을 요청당 한 번만 조회해 모델에 넣는다.
 *
 * 이전에는 header/footer/admin-header/corp-header 4개 fragment가 각자
 * th:with로 서비스를 호출했다. 한 화면에 헤더와 푸터가 같이 있으면
 * 같은 조회가 두 번 일어나고, 설정값을 쓰는 자리를 늘릴수록 더 늘어난다.
 * 여기서 한 번 담아두면 fragment는 model attribute만 읽으면 된다.
 *
 * 같은 이름(siteSetting)을 관리자 사이트 설정 화면도 그대로 사용한다.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class SiteSettingAdvice {

    private final SiteSettingQueryService siteSettingQueryService;

    @ModelAttribute("siteSetting")
    public SiteSettingView siteSetting() {
        return siteSettingQueryService.getSiteSettingView();
    }
}
