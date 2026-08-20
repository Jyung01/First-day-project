package kr.co.firstdayproject.controller.common;

import kr.co.firstdayproject.repository.site.SiteVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class SiteVersionAdvice {

    private static final String DEFAULT_VERSION = "1.0.0";

    private final SiteVersionRepository siteVersionRepository;

    @ModelAttribute("siteVersionName")
    public String siteVersionName() {
        return siteVersionRepository
                .findFirstByOrderByCreatedAtDescSiteVersionIdDesc()
                .map(version -> version.getVersionName())
                .orElse(DEFAULT_VERSION);
    }
}
