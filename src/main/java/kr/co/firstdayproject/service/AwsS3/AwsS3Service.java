package kr.co.firstdayproject.service.AwsS3;

import kr.co.firstdayproject.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3Service {

    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    /**
     * MultipartFile을 AWS S3에 업로드하고, 접근 가능한 CloudFront URL을 반환합니다.
     *
     * @param file 업로드할 파일 (MultipartFile)
     * @return CloudFront를 통해 접근할 수 있는 파일의 전체 URL
     * @throws IOException 파일의 바이트를 읽어오는 과정에서 예외가 발생할 수 있습니다.
     */
    public String upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    public String upload(MultipartFile file, String directory) throws IOException {

        // 1. 원본 파일명 중복 방지를 위해 UUID를 앞에 붙여 고유한 파일명 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectKey = directory == null || directory.isBlank()
                ? fileName
                : directory.replaceAll("^/+|/+$", "") + "/" + fileName;

        // 2. S3에 파일 업로드 요청 실행
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsProperties.s3().publicBucket()) // 업로드할 S3 버킷 이름
                        .key(objectKey)                             // S3에 저장될 파일 경로 및 이름
                        .contentType(file.getContentType())         // 파일의 미디어 타입 (MIME 타입) 지정
                        .build(),
                RequestBody.fromBytes(file.getBytes())              // 파일 데이터를 바이트 배열로 변환하여 전달
        );

        // 3. S3 파일에 접근할 수 있는 CloudFront 도메인 기반의 URL 조합 후 반환
        return "https://"
                + awsProperties.cloudfront().domain()
                + "/"
                + objectKey;
    }

    /**
     * MultipartFile을 비공개 S3 버킷에 업로드하고, 저장된 objectKey(storageKey)를 반환합니다.
     * CloudFront/공개 URL을 만들지 않으므로, 조회 시점마다 {@link #getPresignedUrl(String)}로
     * 임시 접근 URL을 발급해야 합니다.
     */
    public String uploadPrivate(MultipartFile file, String directory) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectKey = directory == null || directory.isBlank()
                ? fileName
                : directory.replaceAll("^/+|/+$", "") + "/" + fileName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsProperties.s3().privateBucket())
                        .key(objectKey)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        return objectKey;
    }

    /**
     * 비공개 버킷에 저장된 objectKey에 대해 만료시간이 있는 임시 접근 URL을 발급합니다.
     */
    public String getPresignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(awsProperties.s3().privateBucket())
                        .key(objectKey)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 비공개 파일을 원본 파일명으로 내려받을 수 있는 임시 URL을 발급합니다.
     */
    public String getPresignedDownloadUrl(String objectKey, String originalName) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String disposition = ContentDisposition.attachment()
                .filename(originalName, StandardCharsets.UTF_8)
                .build()
                .toString();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(awsProperties.s3().privateBucket())
                        .key(objectKey)
                        .responseContentDisposition(disposition)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * 비공개 버킷에 저장된 objectKey의 파일을 삭제합니다. key가 없으면 아무 동작도 하지 않습니다.
     */
    public void deletePrivate(String objectKey) {
        delete(awsProperties.s3().privateBucket(), objectKey);
    }

    /**
     * {@link #upload(MultipartFile, String)}로 발급된 CloudFront URL로부터 objectKey를 역산해
     * 공개 버킷의 파일을 삭제합니다. CloudFront 도메인으로 시작하지 않는 값(예: 레거시 데이터,
     * 외부 URL)은 안전하게 무시합니다.
     */
    public void deletePublicByUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        String prefix = "https://" + awsProperties.cloudfront().domain() + "/";
        if (!url.startsWith(prefix)) {
            return;
        }

        delete(awsProperties.s3().publicBucket(), url.substring(prefix.length()));
    }

    /**
     * 비공개 파일 교체에 따른 삭제를 현재 DB 트랜잭션의 결과에 맞춰 처리합니다.
     * 커밋되면 이전 파일을, 롤백되면 새로 업로드한 파일을 삭제합니다.
     */
    public void synchronizePrivateReplacement(
            String previousObjectKey,
            String uploadedObjectKey
    ) {
        synchronizeReplacement(
                () -> deletePrivate(previousObjectKey),
                () -> deletePrivate(uploadedObjectKey),
                "비공개 파일"
        );
    }

    /**
     * 공개 파일 교체에 따른 삭제를 현재 DB 트랜잭션의 결과에 맞춰 처리합니다.
     * 커밋되면 이전 파일을, 롤백되면 새로 업로드한 파일을 삭제합니다.
     */
    public void synchronizePublicReplacement(
            String previousUrl,
            String uploadedUrl
    ) {
        synchronizeReplacement(
                () -> deletePublicByUrl(previousUrl),
                () -> deletePublicByUrl(uploadedUrl),
                "공개 파일"
        );
    }

    private void synchronizeReplacement(
            Runnable deletePrevious,
            Runnable deleteUploaded,
            String fileType
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safelyDelete(deletePrevious, fileType + " 기존 파일");
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        safelyDelete(deletePrevious, fileType + " 기존 파일");
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            safelyDelete(deleteUploaded, fileType + " 신규 파일");
                        }
                    }
                }
        );
    }

    private void safelyDelete(Runnable deletion, String targetDescription) {
        try {
            deletion.run();
        } catch (RuntimeException exception) {
            log.warn("S3 {} 삭제에 실패했습니다. 추후 정리가 필요합니다.", targetDescription, exception);
        }
    }

    private void delete(String bucket, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build()
        );
    }
}
