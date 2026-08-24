package kr.co.firstdayproject.service.admin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.firstdayproject.entity.site.SiteSetting;
import kr.co.firstdayproject.repository.site.SiteSettingRepository;
import kr.co.firstdayproject.service.AwsS3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AdminSiteSettingServiceTest {

    private static final String PREVIOUS_URL =
            "https://cdn.example.net/site/brand-images/old-favicon.png";
    private static final String NEW_URL =
            "https://cdn.example.net/site/brand-images/new-favicon.png";

    private SiteSetting brandImageSetting;
    private SiteSettingRepository siteSettingRepository;
    private AwsS3Service awsS3Service;
    private AdminSiteSettingService adminSiteSettingService;

    @BeforeEach
    void setUp() throws Exception {
        siteSettingRepository = mock(SiteSettingRepository.class);
        awsS3Service = mock(AwsS3Service.class);
        adminSiteSettingService =
                new AdminSiteSettingService(siteSettingRepository, awsS3Service);

        when(awsS3Service.upload(any(), anyString())).thenReturn(NEW_URL);
        brandImageSetting = SiteSetting.builder()
                .settingKey("brand_image")
                .settingValue("{\"faviconUrl\":\"" + PREVIOUS_URL + "\"}")
                .build();
        when(siteSettingRepository.findById("brand_image"))
                .thenReturn(Optional.of(brandImageSetting));
    }

    /**
     * 트랜잭션 안에서 곧바로 지우면, 이후 롤백됐을 때 DB는 이전 URL로 되돌아가는데
     * 파일은 이미 없어져 복구 불가능한 깨진 이미지가 된다. 삭제 시점은 반드시
     * 트랜잭션 결과에 맡겨야 한다.
     */
    @Test
    void defersPreviousImageDeletionToTransactionResult() {
        adminSiteSettingService.updateImage(1L, "FAVICON", imageFile());

        verify(awsS3Service).synchronizePublicReplacement(PREVIOUS_URL, NEW_URL);
        verify(awsS3Service, never()).deletePublicByUrl(anyString());
    }

    @Test
    void storesUploadedUrlUnderMatchingField() {
        adminSiteSettingService.updateImage(1L, "FAVICON", imageFile());

        assertThat(brandImageSetting.getSettingValue())
                .contains(NEW_URL)
                .doesNotContain(PREVIOUS_URL);
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "imageFile",
                "favicon.png",
                "image/png",
                new byte[] {1, 2, 3}
        );
    }
}
