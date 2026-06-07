package com.smartinventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.model.Order;
import com.smartinventory.service.OrderService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true")
public class OrderStatusListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @SqsListener("${aws.sqs.order-queue-url:order-events}")
    public void handleOrderEvent(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long orderId = Long.valueOf(event.get("orderId").toString());
            String statusStr = event.get("status").toString();

            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(statusStr);
            orderService.updateOrderStatus(orderId, newStatus, "SYSTEM");
            log.info("Order {} status updated to {}", orderId, newStatus);
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage());
        }
    }
}
