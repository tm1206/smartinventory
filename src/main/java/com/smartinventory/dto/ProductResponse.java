package com.smartinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String sku;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
