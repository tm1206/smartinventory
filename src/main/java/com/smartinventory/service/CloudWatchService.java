package com.smartinventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudWatchService {

    private final CloudWatchClient cloudWatchClient;

    @Value("${cloudwatch.log-group:smartinventory-logs}")
    private String logGroup;

    @Value("${cloud.aws.region.static:ap-south-1}")
    private String region;

    public void logApiRequest(String endpoint, String username, int httpStatus) {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName("ApiRequest")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(logGroup)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
            log.info("CloudWatch metric sent: endpoint={}, user={}, status={}", endpoint, username, httpStatus);
        } catch (Exception e) {
            log.warn("Failed to send CloudWatch metric: {}", e.getMessage());
        }
    }

    public void logLoginAttempt(String username, boolean success) {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName(success ? "LoginSuccess" : "LoginFailure")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(logGroup)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
            log.info("CloudWatch login metric: user={}, success={}", username, success);
        } catch (Exception e) {
            log.warn("Failed to send login metric to CloudWatch: {}", e.getMessage());
        }
    }

    public void logException(String exceptionType, String endpoint, String message) {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName("ApplicationException")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(Instant.now())
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(logGroup)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
            log.error("Exception logged to CloudWatch: type={}, endpoint={}, message={}",
                    exceptionType, endpoint, message);
        } catch (Exception e) {
            log.warn("Failed to log exception to CloudWatch: {}", e.getMessage());
        }
    }
}
