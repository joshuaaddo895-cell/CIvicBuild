package backend.example.civicbuild.catalog.controller;

import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.catalog.dto.CategoryResponse;
import backend.example.civicbuild.catalog.dto.CreateProductRequest;
import backend.example.civicbuild.catalog.dto.ProductResponse;
import backend.example.civicbuild.catalog.dto.SupplierResponse;
import backend.example.civicbuild.catalog.dto.UpdateProductRequest;
import backend.example.civicbuild.catalog.service.CatalogService;
import backend.example.civicbuild.common.dto.ApiResponse;
import backend.example.civicbuild.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.listCategories()));
    }

    @GetMapping("/api/suppliers")
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponse>>> listSuppliers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.listSuppliers(q, category, page, limit)));
    }

    @GetMapping("/api/suppliers/{supplierId}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getSupplier(supplierId)));
    }

    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                catalogService.listProducts(q, category, supplierId, agencyId, page, limit)));
    }

    @GetMapping("/api/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getProduct(productId)));
    }

    @PostMapping("/api/agencies/me/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", catalogService.createAgencyProduct(user, request)));
    }

    @PatchMapping("/api/agencies/me/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Product updated", catalogService.updateAgencyProduct(user, productId, request)));
    }

    @DeleteMapping("/api/agencies/me/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID productId) {
        catalogService.deleteAgencyProduct(user, productId);
        return ResponseEntity.ok(ApiResponse.message("Product deleted"));
    }

    @PostMapping("/api/agencies/me/products/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = catalogService.uploadProductImage(user, file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("imageUrl", imageUrl)));
    }
}
