package kr.co.firstdayproject.service.AwsS3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import kr.co.firstdayproject.config.properties.AwsProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class AwsS3ServiceTest {

    private static final AwsProperties PROPERTIES = new AwsProperties(
            "ap-northeast-2",
            new AwsProperties.S3("public-bucket", "private-bucket"),
            new AwsProperties.CloudFront("cdn.firstday.test")
    );

    @Test
    void uploadsCompanyLogoUnderCompaniesLogoDirectory() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);
        AwsS3Service service = new AwsS3Service(s3Client, s3Presigner, PROPERTIES);
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

    @Test
    void uploadsProfileImageToPrivateBucketAndReturnsObjectKey() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);
        AwsS3Service service = new AwsS3Service(s3Client, s3Presigner, PROPERTIES);
        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "face.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String objectKey = service.uploadPrivate(profileImage, "personal_profile");

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("private-bucket");
        assertThat(objectKey)
                .startsWith("personal_profile/")
                .endsWith("_face.png");
        assertThat(objectKey).doesNotContain("https://");
    }

    @Test
    void issuesPresignedUrlForStoredObjectKey() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(
                "https://private-bucket.s3.test/personal_profile/key.png?X-Amz-Signature=abc"
        ).toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        AwsS3Service service = new AwsS3Service(s3Client, s3Presigner, PROPERTIES);

        String url = service.getPresignedUrl("personal_profile/key.png");

        assertThat(url).contains("X-Amz-Signature=abc");
    }

    @Test
    void returnsNullPresignedUrlWhenObjectKeyIsMissing() {
        S3Client s3Client = mock(S3Client.class);
        S3Presigner s3Presigner = mock(S3Presigner.class);
        AwsS3Service service = new AwsS3Service(s3Client, s3Presigner, PROPERTIES);

        assertThat(service.getPresignedUrl(null)).isNull();
        assertThat(service.getPresignedUrl(" ")).isNull();
    }
}
