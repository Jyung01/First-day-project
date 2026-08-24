package kr.co.firstdayproject.service.admin.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kr.co.firstdayproject.dto.admin.config.SiteSettingView;
import kr.co.firstdayproject.entity.site.SiteSetting;
import kr.co.firstdayproject.repository.site.SiteSettingRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * site_settings(key-value/JSON) 테이블을 다루는 서비스.
 * row 3개를 고정 키로 사용한다: basic_info / footer / brand_image
 * 각 row의 setting_value는 Map<String, Object> 형태 그대로 JSON 직렬화한다.
 *
 * 브랜드 이미지(헤더/푸터 로고, 파비콘)는 공개적으로 노출되는 리소스이므로
 * 로컬 디스크(src/main/resources/static 등)에 저장하지 않고 S3 공개 버킷에 업로드한다.
 * 저장/조회 모두 AwsS3Service의 CloudFront URL을 그대로 사용한다.
 */
@Service
@Transactional(readOnly = true)
public class AdminSiteSettingService {

    private static final String KEY_BASIC_INFO = "basic_info";
    private static final String KEY_FOOTER = "footer";
    private static final String KEY_BRAND_IMAGE = "brand_image";

    /** S3 내 브랜드 이미지 저장 디렉토리 (버킷 하위 prefix) */
    private static final String S3_BRAND_IMAGE_DIR = "site/brand-images";

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

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/svg+xml",
            "image/x-icon",
            "image/vnd.microsoft.icon"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    private final SiteSettingRepository siteSettingRepository;
    private final AwsS3Service awsS3Service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminSiteSettingService(
            SiteSettingRepository siteSettingRepository,
            AwsS3Service awsS3Service
    ) {
        this.siteSettingRepository = siteSettingRepository;
        this.awsS3Service = awsS3Service;
    }

    public SiteSettingView getSiteSettingView() {
        Map<String, Object> basicInfo = readSetting(KEY_BASIC_INFO, BASIC_INFO_DEFAULTS);
        Map<String, Object> footer = readSetting(KEY_FOOTER, FOOTER_DEFAULTS);
        Map<String, Object> brandImage = readSetting(KEY_BRAND_IMAGE, Map.of());

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

    @Transactional
    public void updateBasicInfo(
            Long adminUserId,
            String serviceName,
            String supportEmail,
            String supportPhone,
            String serviceHours,
            String serviceDescription
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("serviceName", requireText(serviceName, "서비스명을 입력해주세요."));
        value.put("supportEmail", requireText(supportEmail, "고객센터 이메일을 입력해주세요."));
        value.put("supportPhone", requireText(supportPhone, "대표 전화번호를 입력해주세요."));
        value.put("serviceHours", requireText(serviceHours, "운영시간을 입력해주세요."));
        value.put(
                "serviceDescription",
                serviceDescription == null ? "" : serviceDescription.strip()
        );

        saveSetting(KEY_BASIC_INFO, value, adminUserId);
    }

    @Transactional
    public void updateFooter(
            Long adminUserId,
            String companyName,
            String businessNumber,
            String companyAddress,
            String copyrightText
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("companyName", requireText(companyName, "회사명을 입력해주세요."));
        value.put("businessNumber", requireText(businessNumber, "사업자등록번호를 입력해주세요."));
        value.put("companyAddress", requireText(companyAddress, "주소를 입력해주세요."));
        value.put(
                "copyrightText",
                copyrightText == null ? "" : copyrightText.strip()
        );

        saveSetting(KEY_FOOTER, value, adminUserId);
    }

    /**
     * 브랜드 이미지(헤더 로고/푸터 로고/파비콘)를 S3 공개 버킷에 업로드하고,
     * 기존에 등록되어 있던 이미지가 있으면 트랜잭션 커밋 후 삭제한다.
     */
    @Transactional
    public void updateImage(
            Long adminUserId,
            String imageType,
            MultipartFile file
    ) {
        validateFile(file);

        String field = switch (imageType) {
            case "HEADER_LOGO" -> "headerLogoUrl";
            // 푸터 로고는 다크 배경 전용이라 공통 푸터와 관리자 헤더가 함께 사용한다.
            // 이 값을 바꾸면 두 화면이 동시에 바뀐다.
            case "FOOTER_LOGO" -> "footerLogoUrl";
            case "FAVICON" -> "faviconUrl";
            default -> throw new IllegalArgumentException(
                    "올바르지 않은 이미지 종류입니다."
            );
        };

        Map<String, Object> current = readSetting(KEY_BRAND_IMAGE, Map.of());
        String previousUrl = asText(current, field);

        String newUrl;
        try {
            newUrl = awsS3Service.upload(file, S3_BRAND_IMAGE_DIR);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "이미지 저장 중 오류가 발생했습니다.",
                    exception
            );
        }

        Map<String, Object> updated = new LinkedHashMap<>(current);
        updated.put(field, newUrl);
        saveSetting(KEY_BRAND_IMAGE, updated, adminUserId);

        // 파일 정리는 트랜잭션 결과에 맡긴다. 커밋되면 이전 이미지를,
        // 롤백되면 방금 올린 이미지를 지운다. 여기서 바로 지우면
        // 이후 롤백 시 DB는 이전 URL로 되돌아가는데 파일은 이미 없어져
        // 복구할 수 없는 깨진 이미지가 된다.
        awsS3Service.synchronizePublicReplacement(previousUrl, newUrl);
    }

    private Map<String, Object> readSetting(
            String key,
            Map<String, String> defaults
    ) {
        Optional<SiteSetting> setting = siteSettingRepository.findById(key);

        if (setting.isEmpty()) {
            return new LinkedHashMap<>(defaults);
        }

        try {
            return objectMapper.readValue(
                    setting.get().getSettingValue(),
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "설정 값을 읽는 중 오류가 발생했습니다. (key=" + key + ")",
                    exception
            );
        }
    }

    private void saveSetting(String key, Map<String, Object> value, Long adminUserId) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("관리자 로그인이 필요합니다.");
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "설정 값을 저장하는 중 오류가 발생했습니다.",
                    exception
            );
        }

        SiteSetting setting = siteSettingRepository.findById(key).orElse(null);

        if (setting == null) {
            siteSettingRepository.save(
                    SiteSetting.builder()
                            .settingKey(key)
                            .settingValue(json)
                            .updatedBy(adminUserId)
                            .updatedAt(LocalDateTime.now())
                            .build()
            );
        } else {
            setting.setSettingValue(json);
            setting.setUpdatedBy(adminUserId);
            setting.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String asText(Map<String, Object> map, String field) {
        Object value = map.get(field);
        return value == null ? null : value.toString();
    }

    private String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.strip();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일을 선택해주세요.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "파일 크기는 2MB 이하로 업로드해주세요."
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "PNG, JPG, SVG, ICO 형식의 이미지만 업로드할 수 있습니다."
            );
        }
    }
}