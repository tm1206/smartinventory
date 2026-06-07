package com.smartinventory.controller;

import com.smartinventory.dto.ApiResponse;
import com.smartinventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory")
    @Operation(summary = "Get inventory summary report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventorySummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getInventorySummary()));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get orders report by date range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrdersReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOrdersReport(start, end)));
    }

    @GetMapping("/inventory/export")
    @Operation(summary = "Export inventory as CSV and upload to S3")
    public ResponseEntity<ApiResponse<String>> exportInventoryCsv() {
        String presignedUrl = reportService.exportInventoryCsv();
        return ResponseEntity.ok(ApiResponse.success("Inventory exported successfully", presignedUrl));
    }

    @GetMapping("/orders/export")
    @Operation(summary = "Export orders as CSV and upload to S3")
    public ResponseEntity<ApiResponse<String>> exportOrdersCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        String presignedUrl = reportService.exportOrdersCsv(start, end);
        return ResponseEntity.ok(ApiResponse.success("Orders exported successfully", presignedUrl));
    }
}
