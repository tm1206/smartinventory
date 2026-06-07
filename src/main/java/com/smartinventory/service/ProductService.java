package com.smartinventory.service;

import com.smartinventory.dto.ProductRequest;
import com.smartinventory.dto.ProductResponse;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final S3Service s3Service;
    private final SQSService sqsService;
    private final AuditLogService auditLogService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .build();

        product = productRepository.save(product);

        if (product.getQuantity() < LOW_STOCK_THRESHOLD) {
            publishLowStockAlert(product);
        }

        auditLogService.log(username, "PRODUCT_CREATED", "Product",
                product.getId().toString(), "Product created: " + product.getName(), null);

        return toResponse(product);
    }

    public Page<ProductResponse> getAllProducts(String name, String category, Pageable pageable) {
        String searchName = (name != null && !name.isBlank()) ? name : "";
        String searchCategory = (category != null && !category.isBlank()) ? category : "";
        return productRepository.searchProducts(searchName, searchCategory, pageable)
                .map(this::toResponse);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toResponse(product);
    }

    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        int oldQuantity = product.getQuantity();
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setQuantity(request.getQuantity());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        product = productRepository.save(product);

        if (product.getQuantity() < LOW_STOCK_THRESHOLD && oldQuantity >= LOW_STOCK_THRESHOLD) {
            publishLowStockAlert(product);
        }

        auditLogService.log(username, "PRODUCT_UPDATED", "Product",
                product.getId().toString(), "Product updated: " + product.getName(), null);

        return toResponse(product);
    }

    @Transactional
    public String uploadProductImage(Long id, MultipartFile file, String username) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String imageUrl = s3Service.uploadFile(file, "products");
        product.setImageUrl(imageUrl);
        productRepository.save(product);

        auditLogService.log(username, "PRODUCT_IMAGE_UPLOADED", "Product",
                id.toString(), "Image uploaded for product: " + product.getName(), null);

        return imageUrl;
    }

    @Transactional
    public void deleteProduct(Long id, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        productRepository.delete(product);

        auditLogService.log(username, "PRODUCT_DELETED", "Product",
                id.toString(), "Product deleted: " + product.getName(), null);
    }

    @Transactional
    public void deductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getQuantity());
        }

        product.setQuantity(product.getQuantity() - quantity);
        product = productRepository.save(product);

        if (product.getQuantity() < LOW_STOCK_THRESHOLD) {
            publishLowStockAlert(product);
        }
    }

    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findByQuantityLessThan(LOW_STOCK_THRESHOLD)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void publishLowStockAlert(Product product) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("productId", product.getId());
            alert.put("productName", product.getName());
            alert.put("sku", product.getSku());
            alert.put("currentQuantity", product.getQuantity());
            alert.put("threshold", LOW_STOCK_THRESHOLD);
            sqsService.publishStockAlert(alert);
        } catch (Exception e) {
            log.warn("Failed to publish low stock alert: {}", e.getMessage());
        }
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .category(product.getCategory())
                .quantity(product.getQuantity())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .lowStock(product.getQuantity() < LOW_STOCK_THRESHOLD)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
