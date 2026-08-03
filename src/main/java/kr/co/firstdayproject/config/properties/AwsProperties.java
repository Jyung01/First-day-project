package kr.co.firstdayproject.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(
        String region,
        S3 s3,
        CloudFront cloudfront
) {

    public record S3(
            String publicBucket,
            String privateBucket
    ) {
    }

    public record CloudFront(
            String domain
    ) {
    }
}