package kr.co.firstdayproject.service.AwsS3;

import kr.co.firstdayproject.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /**
     * MultipartFile을 AWS S3에 업로드하고, 접근 가능한 CloudFront URL을 반환합니다.
     *
     * @param file 업로드할 파일 (MultipartFile)
     * @return CloudFront를 통해 접근할 수 있는 파일의 전체 URL
     * @throws IOException 파일의 바이트를 읽어오는 과정에서 예외가 발생할 수 있습니다.
     */
    public String upload(MultipartFile file) throws IOException {

        // 1. 원본 파일명 중복 방지를 위해 UUID를 앞에 붙여 고유한 파일명 생성
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 2. S3에 파일 업로드 요청 실행
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(awsProperties.s3().publicBucket()) // 업로드할 S3 버킷 이름
                        .key(fileName)                              // S3에 저장될 파일 경로 및 이름
                        .contentType(file.getContentType())         // 파일의 미디어 타입 (MIME 타입) 지정
                        .build(),
                RequestBody.fromBytes(file.getBytes())              // 파일 데이터를 바이트 배열로 변환하여 전달
        );

        // 3. S3 파일에 접근할 수 있는 CloudFront 도메인 기반의 URL 조합 후 반환
        return "https://"
                + awsProperties.cloudfront().domain()
                + "/"
                + fileName;
    }
}