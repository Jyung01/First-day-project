package kr.co.firstdayproject.service.site;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.admin.config.SiteSettingView;
import kr.co.firstdayproject.entity.site.SiteSetting;
import kr.co.firstdayproject.repository.site.SiteSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * site_settings 읽기 전용 조회.
 *
 * 사이트 설정은 관리자만 "수정"하지만 "조회"는 공통 헤더·푸터를 통해
 * 모든 화면이 한다. 그래서 조회를 AdminSiteSettingService에서 떼어냈다.
 * 일반 사용자 화면이 Admin~ 서비스를 호출하는 구조를 없애기 위한 분리다.
 *
 * 화면에서 직접 부르지 말고 SiteSettingAdvice가 넣어주는
 * model attribute "siteSetting"을 쓴다. 템플릿마다 호출하면
 * 한 페이지(헤더 + 푸터)에서 같은 조회가 여러 번 일어난다.
 */
@Service
@Transactional(readOnly = true)
public class SiteSettingQueryService {

    public static final String KEY_BASIC_INFO = "basic_info";
    public static final String KEY_FOOTER = "footer";
    public static final String KEY_BRAND_IMAGE = "brand_image";

    /**
     * DB에 해당 row가 아직 없을 때 사용하는 값.
     * 새 환경에 배포한 직후에도 화면이 비지 않도록 하는 안전망이며,
     * 관리자가 한 번 저장하면 그때부터는 DB 값이 우선한다.
     */
    private static final Map<String, String> BASIC_INFO_DEFAULTS = Map.of(
            "serviceName", "첫출근",
            "supportEmail", "help@firstwork.co.kr",
            "supportPhone", "02-1234-5678",
            "serviceHours", "평일 09:00 ~ 18:00",
            "serviceDescription", "설레는 첫 출근을 함께 준비합니다."
    );

    private static final Map<String, String> FOOTER_DEFAULTS = Map.of(
            "companyName", "첫출근",
            "businessNumber", "",
            "companyAddress", "",
            "copyrightText", ""
    );

    private final SiteSettingRepository siteSettingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SiteSettingQueryService(SiteSettingRepository siteSettingRepository) {
        this.siteSettingRepository = siteSettingRepository;
    }

    /**
     * 3개 row를 findById로 따로 읽지 않고 한 번에 가져온다.
     * 이 메서드는 SiteSettingAdvice를 통해 모든 요청에서 호출되므로
     * 쿼리 수가 그대로 요청당 비용이 된다.
     */
    public SiteSettingView getSiteSettingView() {
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        siteSettingRepository
                .findAllById(List.of(KEY_BASIC_INFO, KEY_FOOTER, KEY_BRAND_IMAGE))
                .forEach(setting -> rows.put(
                        setting.getSettingKey(),
                        parseValue(setting)
                ));

        Map<String, Object> basicInfo = withDefaults(rows.get(KEY_BASIC_INFO), BASIC_INFO_DEFAULTS);
        Map<String, Object> footer = withDefaults(rows.get(KEY_FOOTER), FOOTER_DEFAULTS);
        Map<String, Object> brandImage = withDefaults(rows.get(KEY_BRAND_IMAGE), Map.of());

        return new SiteSettingView(
                asText(basicInfo, "serviceName"),
                asText(basicInfo, "supportEmail"),
                asText(basicInfo, "supportPhone"),
                asText(basicInfo, "serviceHours"),
                asText(basicInfo, "serviceDescription"),
                asText(footer, "companyName"),
                asText(footer, "businessNumber"),
                asText(footer, "companyAddress"),
                asText(footer, "copyrightText"),
                asText(brandImage, "headerLogoUrl"),
                asText(brandImage, "footerLogoUrl"),
                asText(brandImage, "faviconUrl")
        );
    }

    /** 특정 row 하나만 필요할 때 쓴다. (브랜드 이미지 교체 등) */
    public Map<String, Object> readSetting(String key, Map<String, String> defaults) {
        return withDefaults(
                siteSettingRepository.findById(key).map(this::parseValue).orElse(null),
                defaults
        );
    }

    private Map<String, Object> withDefaults(
            Map<String, Object> value,
            Map<String, String> defaults
    ) {
        return value != null ? value : new LinkedHashMap<>(defaults);
    }

    private Map<String, Object> parseValue(SiteSetting setting) {
        try {
            return objectMapper.readValue(
                    setting.getSettingValue(),
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "설정 값을 읽는 중 오류가 발생했습니다. (key="
                            + setting.getSettingKey() + ")",
                    exception
            );
        }
    }

    public String asText(Map<String, Object> map, String field) {
        Object value = map.get(field);
        return value == null ? null : value.toString();
    }
}
