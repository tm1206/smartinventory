package com.smartinventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.service.SESService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true")
public class StockAlertListener {

    private final SESService sesService;
    private final ObjectMapper objectMapper;

    @Value("${aws.ses.alert-email:admin@smartinventory.com}")
    private String alertEmail;

    @SqsListener("${aws.sqs.stock-queue-url:stock-alerts}")
    public void handleStockAlert(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> alert = objectMapper.readValue(message, Map.class);
            String productName = alert.get("productName").toString();
            int quantity = Integer.parseInt(alert.get("currentQuantity").toString());

            sesService.sendLowStockAlertEmail(alertEmail, productName, quantity);
            log.info("Low stock alert processed for product: {}, quantity: {}", productName, quantity);
        } catch (Exception e) {
            log.error("Failed to process stock alert: {}", e.getMessage());
        }
    }
}
