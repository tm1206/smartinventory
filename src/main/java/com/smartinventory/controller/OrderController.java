package com.smartinventory.controller;

import com.smartinventory.dto.ApiResponse;
import com.smartinventory.dto.OrderRequest;
import com.smartinventory.dto.OrderResponse;
import com.smartinventory.model.Order;
import com.smartinventory.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse response = orderService.placeOrder(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all orders (Admin/Manager only)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getUserOrders(userDetails.getUsername(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByNumber(orderNumber)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        OrderResponse response = orderService.updateOrderStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", response));
    }
}
