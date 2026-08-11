package kr.co.firstdayproject.dto.admin.config;

/**
 * site-setting.html 화면에 그대로 바인딩하기 위한 통합 뷰.
 * basic_info + footer + brand_image 3개 JSON row를 합쳐서 만든다.
 */
public record SiteSettingView(
        String serviceName,
        String supportEmail,
        String supportPhone,
        String serviceHours,
        String serviceDescription,
        String companyName,
        String businessNumber,
        String companyAddress,
        String copyrightText,
        String headerLogoUrl,
        String footerLogoUrl,
        String faviconUrl
) {
}