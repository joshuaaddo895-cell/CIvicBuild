package backend.example.civicbuild.order.service;

import backend.example.civicbuild.catalog.entity.Product;
import backend.example.civicbuild.catalog.repository.ProductRepository;
import backend.example.civicbuild.common.exception.NotFoundException;
import backend.example.civicbuild.order.dto.CheckoutRequest;
import backend.example.civicbuild.order.entity.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CheckoutItemResolver {

    private final ProductRepository productRepository;
    private final OrderTotalCalculator totalCalculator;

    public CheckoutItemResolver(ProductRepository productRepository, OrderTotalCalculator totalCalculator) {
        this.productRepository = productRepository;
        this.totalCalculator = totalCalculator;
    }

    public OrderItem resolve(CheckoutRequest.CheckoutItemRequest item) {
        if (item.productId() == null) {
            return totalCalculator.toOrderItem(item);
        }
        Product product = productRepository
                .findById(item.productId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + item.productId()));
        if (!product.isActive() || product.getStockQuantity() < item.quantity().intValue()) {
            throw new IllegalStateException("Product unavailable or insufficient stock: " + product.getName());
        }
        String supplierName = product.getSupplier() != null
                ? product.getSupplier().getName()
                : product.getAgency() != null ? product.getAgency().getName() : item.supplierName();
        OrderItem orderItem = totalCalculator.toOrderItem(new CheckoutRequest.CheckoutItemRequest(
                product.getId(),
                product.getName(),
                supplierName,
                product.getPrice(),
                item.quantity(),
                product.getUnit()));
        if (product.getAgency() != null) {
            orderItem.setAgencyId(product.getAgency().getId());
        }
        return orderItem;
    }

    public BigDecimal resolvedUnitPrice(CheckoutRequest.CheckoutItemRequest item) {
        if (item.productId() == null) {
            return item.unitPrice();
        }
        Product product = productRepository
                .findById(item.productId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + item.productId()));
        return product.getPrice();
    }
}
