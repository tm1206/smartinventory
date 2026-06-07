package com.smartinventory.controller;

import com.smartinventory.dto.ApiResponse;
import com.smartinventory.dto.ProductRequest;
import com.smartinventory.dto.ProductResponse;
import com.smartinventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProductResponse response = productService.createProduct(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all products with optional filters")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        Page<ProductResponse> products = productService.getAllProducts(name, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySku(sku)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProductResponse response = productService.updateProduct(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image to S3")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        String imageUrl = productService.uploadProductImage(id, file, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", imageUrl));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        productService.deleteProduct(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all product categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllCategories()));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get products with low stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getLowStockProducts()));
    }
}
