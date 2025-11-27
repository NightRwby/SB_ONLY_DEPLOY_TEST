package com.example.demo.config; // config 폴더에 생성 권장

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    // application.properties에서 값을 주입받습니다.
    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * AmazonS3Client 객체를 생성하여 Spring Bean으로 등록합니다.
     */
    @Bean
    public AmazonS3 amazonS3() {
        // 1. AWS 자격 증명 설정
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        // 2. S3 클라이언트 빌더를 사용하여 객체 생성
        return AmazonS3ClientBuilder.standard()
                // 자격 증명을 클라이언트에 제공
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                // 리전 설정 (문자열 region을 Regions Enum으로 변환하여 사용)
                .withRegion(Regions.fromName(region))
                .build();
    }

    /**
     * 버킷 이름을 FileService 등 다른 곳에서 주입받기 위해 String Bean으로 등록합니다.
     */
    @Bean
    public String s3BucketName() {
        return bucketName;
    }
}