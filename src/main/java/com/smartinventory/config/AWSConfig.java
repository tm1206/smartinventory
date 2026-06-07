package com.smartinventory.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AWSConfig {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:ap-south-1}")
    private String region;

    private boolean hasCredentials() {
        return !accessKey.isBlank() && !secretKey.isBlank();
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (hasCredentials()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return AnonymousCredentialsProvider.create();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    @Primary
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.builder()
                .configure(options -> options.autoStartup(hasCredentials()))
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
