package com.smartinventory.service;

import com.smartinventory.model.Order;
import com.smartinventory.model.Product;
import com.smartinventory.repository.OrderRepository;
import com.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final S3Service s3Service;

    public Map<String, Object> getInventorySummary() {
        List<Product> allProducts = productRepository.findAll();
        List<Product> lowStockProducts = productRepository.findByQuantityLessThan(10);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProducts", allProducts.size());
        summary.put("lowStockCount", lowStockProducts.size());
        summary.put("totalInventoryValue",
                allProducts.stream()
                        .map(p -> p.getPrice().multiply(java.math.BigDecimal.valueOf(p.getQuantity())))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("categories", productRepository.findAllCategories());
        summary.put("lowStockProducts", lowStockProducts.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "sku", p.getSku(),
                "quantity", p.getQuantity()
        )).toList());
        summary.put("generatedAt", LocalDateTime.now().toString());
        return summary;
    }

    public Map<String, Object> getOrdersReport(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByDateRange(start, end);

        Map<String, Object> report = new HashMap<>();
        report.put("totalOrders", orders.size());
        report.put("totalRevenue",
                orders.stream()
                        .map(Order::getTotalAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        report.put("pendingOrders", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count());
        report.put("processingOrders", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PROCESSING).count());
        report.put("completedOrders", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED).count());
        report.put("cancelledOrders", orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED).count());
        report.put("dateRange", Map.of("start", start.toString(), "end", end.toString()));
        report.put("generatedAt", LocalDateTime.now().toString());
        return report;
    }

    public String exportInventoryCsv() {
        List<Product> products = productRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Name,SKU,Category,Quantity,Price,ImageUrl,CreatedAt\n");

        for (Product p : products) {
            csv.append(String.format("%d,%s,%s,%s,%d,%.2f,%s,%s\n",
                    p.getId(),
                    escapeCsv(p.getName()),
                    escapeCsv(p.getSku()),
                    escapeCsv(p.getCategory()),
                    p.getQuantity(),
                    p.getPrice(),
                    p.getImageUrl() != null ? escapeCsv(p.getImageUrl()) : "",
                    p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
            ));
        }

        String filename = "inventory-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
        return s3Service.uploadCsvReport(csv.toString(), filename);
    }

    public String exportOrdersCsv(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByDateRange(start, end);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,OrderNumber,Username,Status,TotalAmount,ShippingAddress,CreatedAt\n");

        for (Order o : orders) {
            csv.append(String.format("%d,%s,%s,%s,%.2f,%s,%s\n",
                    o.getId(),
                    o.getOrderNumber(),
                    o.getUser().getUsername(),
                    o.getStatus().name(),
                    o.getTotalAmount(),
                    o.getShippingAddress() != null ? escapeCsv(o.getShippingAddress()) : "",
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
            ));
        }

        String filename = "orders-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
        return s3Service.uploadCsvReport(csv.toString(), filename);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
