package kr.co.firstdayproject.service.AwsS3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import kr.co.firstdayproject.config.properties.AwsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class AwsS3ServiceTest {

    @Test
    void uploadsCompanyLogoUnderCompaniesLogoDirectory() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        AwsProperties properties = new AwsProperties(
                "ap-northeast-2",
                new AwsProperties.S3("public-bucket", "private-bucket"),
                new AwsProperties.CloudFront("cdn.firstday.test")
        );
        AwsS3Service service = new AwsS3Service(s3Client, properties);
        MockMultipartFile logo = new MockMultipartFile(
                "companyLogo",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String url = service.upload(logo, "companies_logo");

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("public-bucket");
        assertThat(requestCaptor.getValue().key())
                .startsWith("companies_logo/")
                .endsWith("_logo.png");
        assertThat(url)
                .startsWith("https://cdn.firstday.test/companies_logo/")
                .endsWith("_logo.png");
    }
}
