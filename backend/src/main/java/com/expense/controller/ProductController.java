package com.expense.controller;

import com.expense.dto.ApiResponse;
import com.expense.entity.User;
import com.expense.controller.BaseController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController extends BaseController {

    // Mock digital products data
    private final List<Map<String, Object>> digitalProducts = List.of(
        Map.of(
            "id", 1,
            "name", "Financial Freedom Blueprint",
            "description", "Complete guide to achieving financial independence with proven strategies",
            "price", new BigDecimal("47.00"),
            "category", "Ebook",
            "rating", 4.8,
            "students", 1250,
            "image", "📚",
            "features", List.of("Comprehensive guide", "Proven strategies", "1250+ students")
        ),
        Map.of(
            "id", 2,
            "name", "Smart Budget Template Pack",
            "description", "Professional Excel templates for budget tracking and financial planning",
            "price", new BigDecimal("29.00"),
            "category", "Templates",
            "rating", 4.9,
            "students", 890,
            "image", "📊",
            "features", List.of("Excel templates", "Budget tracking", "Financial planning")
        ),
        Map.of(
            "id", 3,
            "name", "Investment Mastery Course",
            "description", "Comprehensive video course on building wealth through smart investing",
            "price", new BigDecimal("97.00"),
            "category", "Course",
            "rating", 4.7,
            "students", 2100,
            "image", "🎥",
            "features", List.of("Video course", "Investment strategies", "Practical examples")
        )
    );

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} requesting products with filters: category={}, priceRange={}-{}", 
                    currentUser.getId(), category, minPrice, maxPrice);
            
            List<Map<String, Object>> filteredProducts = digitalProducts;
            
            // Apply filters
            if (category != null && !category.isEmpty()) {
                filteredProducts = filteredProducts.stream()
                        .filter(product -> category.equals(product.get("category")))
                        .toList();
            }
            
            if (minPrice != null && !minPrice.isEmpty()) {
                try {
                    BigDecimal min = new BigDecimal(minPrice);
                    filteredProducts = filteredProducts.stream()
                            .filter(product -> ((BigDecimal) product.get("price")).compareTo(min) >= 0)
                            .toList();
                } catch (NumberFormatException e) {
                    log.warn("Invalid minPrice format: {}", minPrice);
                }
            }
            
            if (maxPrice != null && !maxPrice.isEmpty()) {
                try {
                    BigDecimal max = new BigDecimal(maxPrice);
                    filteredProducts = filteredProducts.stream()
                            .filter(product -> ((BigDecimal) product.get("price")).compareTo(max) <= 0)
                            .toList();
                } catch (NumberFormatException e) {
                    log.warn("Invalid maxPrice format: {}", maxPrice);
                }
            }
            
            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, filteredProducts.size());
            List<Map<String, Object>> paginatedProducts = filteredProducts.subList(startIndex, endIndex);
            
            Map<String, Object> response = Map.of(
                "products", paginatedProducts,
                "currentPage", page,
                "pageSize", size,
                "totalItems", filteredProducts.size(),
                "totalPages", (int) Math.ceil((double) filteredProducts.size() / size),
                "hasNext", endIndex < filteredProducts.size(),
                "hasPrevious", page > 0
            );
            
        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error retrieving products", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve products: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProduct(@PathVariable Integer id) {
        try {
            User currentUser = getCurrentUser();
            log.info("User {} requesting product details for id: {}", currentUser.getId(), id);
            
            Map<String, Object> product = digitalProducts.stream()
                    .filter(p -> id.equals(p.get("id")))
                    .findFirst()
                    .orElse(null);
            
            if (product == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("Product not found"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error retrieving product", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve product: " + e.getMessage()));
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFeaturedProducts() {
        try {
            User currentUser = getCurrentUser();
            log.info("User {} requesting featured products", currentUser.getId());
            
            // Return top 3 products as featured
            List<Map<String, Object>> featuredProducts = digitalProducts.stream()
                    .limit(3)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(featuredProducts, "Featured products retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error retrieving featured products", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve featured products: " + e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getProductCategories() {
        try {
            User currentUser = getCurrentUser();
            log.info("User {} requesting product categories", currentUser.getId());
            
            List<String> categories = digitalProducts.stream()
                    .map(product -> (String) product.get("category"))
                    .distinct()
                    .sorted()
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Error retrieving product categories", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve categories: " + e.getMessage()));
        }
    }
}
