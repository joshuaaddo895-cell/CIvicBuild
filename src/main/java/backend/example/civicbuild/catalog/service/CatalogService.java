package backend.example.civicbuild.catalog.service;

import backend.example.civicbuild.agency.entity.Agency;
import backend.example.civicbuild.agency.service.AgencyService;
import backend.example.civicbuild.auth.security.AuthenticatedUser;
import backend.example.civicbuild.catalog.dto.CategoryResponse;
import backend.example.civicbuild.catalog.dto.CreateProductRequest;
import backend.example.civicbuild.catalog.dto.ProductResponse;
import backend.example.civicbuild.catalog.dto.SupplierResponse;
import backend.example.civicbuild.catalog.dto.UpdateProductRequest;
import backend.example.civicbuild.catalog.entity.Product;
import backend.example.civicbuild.catalog.repository.CategoryRepository;
import backend.example.civicbuild.catalog.repository.ProductRepository;
import backend.example.civicbuild.catalog.repository.SupplierRepository;
import backend.example.civicbuild.common.dto.PageResponse;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.common.web.PaginationSupport;
import backend.example.civicbuild.storage.DetectedFileType;
import backend.example.civicbuild.storage.FileUploadValidator;
import backend.example.civicbuild.storage.StorageService;
import backend.example.civicbuild.storage.StoredFile;
import backend.example.civicbuild.storage.exception.InvalidFileUploadException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final AgencyService agencyService;
    private final StorageService storageService;
    private final FileUploadValidator fileUploadValidator;

    public CatalogService(
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            AgencyService agencyService,
            StorageService storageService,
            FileUploadValidator fileUploadValidator) {
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.agencyService = agencyService;
        this.storageService = storageService;
        this.fileUploadValidator = fileUploadValidator;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(PaginationSupport.pageableAsc(null, 100, "sortOrder")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> listSuppliers(String q, String category, Integer page, Integer limit) {
        Pageable pageable = PaginationSupport.pageable(page, limit);
        Page<backend.example.civicbuild.catalog.entity.Supplier> result =
                supplierRepository.search(q, category, pageable);
        List<SupplierResponse> items = result.getContent().stream().map(SupplierResponse::from).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(UUID supplierId) {
        return SupplierResponse.from(supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found")));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(
            String q, String category, UUID supplierId, UUID agencyId, Integer page, Integer limit) {
        Pageable pageable = PaginationSupport.pageable(page, limit);
        Page<Product> result = productRepository.search(q, category, supplierId, agencyId, pageable);
        List<ProductResponse> items = result.getContent().stream().map(ProductResponse::from).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        return ProductResponse.from(productRepository
                .findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found")));
    }

    @Transactional
    public ProductResponse createAgencyProduct(AuthenticatedUser actor, CreateProductRequest request) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        Product product = productRepository.save(Product.builder()
                .name(request.name().trim())
                .category(request.category().trim())
                .price(request.price())
                .unit(request.unit().trim())
                .imageUrl(request.imageUrl())
                .description(request.description())
                .agency(agency)
                .stockQuantity(request.stockQuantity())
                .brand(request.brand())
                .spec(request.spec())
                .deliveryEstimate(request.deliveryEstimate())
                .build());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateAgencyProduct(
            AuthenticatedUser actor, UUID productId, UpdateProductRequest request) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        Product product = productRepository
                .findByIdAndAgencyId(productId, agency.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (request.name() != null) product.setName(request.name().trim());
        if (request.category() != null) product.setCategory(request.category().trim());
        if (request.price() != null) product.setPrice(request.price());
        if (request.unit() != null) product.setUnit(request.unit().trim());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.description() != null) product.setDescription(request.description());
        if (request.brand() != null) product.setBrand(request.brand());
        if (request.spec() != null) product.setSpec(request.spec());
        if (request.deliveryEstimate() != null) product.setDeliveryEstimate(request.deliveryEstimate());
        if (request.active() != null) product.setActive(request.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void deleteAgencyProduct(AuthenticatedUser actor, UUID productId) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        Product product = productRepository
                .findByIdAndAgencyId(productId, agency.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        productRepository.delete(product);
    }

    @Transactional
    public String uploadProductImage(AuthenticatedUser actor, MultipartFile file) {
        Agency agency = agencyService.requireOwnedAgency(actor);
        fileUploadValidator.validatePortfolioUpload(file);
        DetectedFileType fileType = fileUploadValidator.detectFileType(file);
        String publicId = "agency-products/" + agency.getId() + "/" + UUID.randomUUID();
        try {
            StoredFile stored = storageService.uploadPublicImage(file.getBytes(), publicId, fileType);
            return stored.deliveryUrl();
        } catch (IOException e) {
            throw new InvalidFileUploadException("Unable to read uploaded file");
        }
    }
}
