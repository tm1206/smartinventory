package com.smartinventory.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:smartinventory-uploads}")
    private String bucketName;

    @Value("${aws.region:ap-south-1}")
    private String region;

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = folder + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, key, inputStream);
        }

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        log.info("File uploaded to S3: {}", url);
        return url;
    }

    public String generatePresignedUrl(String key, Duration duration) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        }
    }

    public String uploadCsvReport(String csvContent, String filename) {
        String key = "reports/" + filename;
        try {
            byte[] bytes = csvContent.getBytes();
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(bytes);
            s3Template.upload(bucketName, key, inputStream);
            log.info("CSV report uploaded to S3: {}", key);
            return generatePresignedUrl(key, Duration.ofHours(1));
        } catch (Exception e) {
            log.error("Failed to upload CSV report to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload report to S3", e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            s3Template.deleteObject(bucketName, key);
            log.info("File deleted from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage());
        }
    }

    private String extractKeyFromUrl(String url) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        return url;
    }
}
