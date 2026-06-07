package com.smartinventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SQSService {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.order-queue-url:}")
    private String orderQueueUrl;

    @Value("${aws.sqs.stock-queue-url:}")
    private String stockQueueUrl;

    public void publishOrderEvent(Map<String, Object> event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            sqsTemplate.send(orderQueueUrl, message);
            log.info("Order event published to SQS: {}", event.get("orderId"));
        } catch (Exception e) {
            log.error("Failed to publish order event to SQS: {}", e.getMessage());
        }
    }

    public void publishStockAlert(Map<String, Object> alert) {
        try {
            String message = objectMapper.writeValueAsString(alert);
            sqsTemplate.send(stockQueueUrl, message);
            log.info("Stock alert published to SQS: {}", alert.get("productId"));
        } catch (Exception e) {
            log.error("Failed to publish stock alert to SQS: {}", e.getMessage());
        }
    }
}
