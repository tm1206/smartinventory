package com.smartinventory.service;

import com.smartinventory.dto.OrderRequest;
import com.smartinventory.dto.OrderResponse;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.model.Order;
import com.smartinventory.model.OrderItem;
import com.smartinventory.model.Product;
import com.smartinventory.model.User;
import com.smartinventory.repository.OrderRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final SQSService sqsService;
    private final SESService sesService;
    private final AuditLogService auditLogService;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemRequest.getProductId()));

            productService.deductStock(product.getId(), itemRequest.getQuantity());

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .build();
            items.add(item);
        }

        order.setItems(items);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        Map<String, Object> orderEvent = new HashMap<>();
        orderEvent.put("orderId", order.getId());
        orderEvent.put("orderNumber", order.getOrderNumber());
        orderEvent.put("userId", user.getId());
        orderEvent.put("username", username);
        orderEvent.put("totalAmount", totalAmount.toString());
        orderEvent.put("status", Order.OrderStatus.PROCESSING.name());
        orderEvent.put("timestamp", LocalDateTime.now().toString());
        sqsService.publishOrderEvent(orderEvent);

        try {
            sesService.sendOrderConfirmationEmail(user.getEmail(), order.getOrderNumber(), totalAmount.toString());
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email: {}", e.getMessage());
        }

        auditLogService.log(username, "ORDER_PLACED", "Order",
                order.getId().toString(), "Order placed: " + order.getOrderNumber(), null);

        return toResponse(order);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<OrderResponse> getUserOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return toResponse(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        order.setStatus(status);
        order = orderRepository.save(order);

        auditLogService.log(username, "ORDER_STATUS_UPDATED", "Order",
                id.toString(), "Status updated to: " + status.name(), null);

        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByDateRange(start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .username(order.getUser().getUsername())
                .items(itemResponses)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
