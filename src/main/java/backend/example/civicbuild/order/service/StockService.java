package backend.example.civicbuild.order.service;

import backend.example.civicbuild.catalog.entity.Product;
import backend.example.civicbuild.catalog.repository.ProductRepository;
import backend.example.civicbuild.order.entity.Order;
import backend.example.civicbuild.order.entity.OrderItem;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void decrementForPaidOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            UUID productId = item.getProductId();
            if (productId == null) {
                continue;
            }
            productRepository.findById(productId).ifPresent(product -> {
                int newStock = Math.max(0, product.getStockQuantity() - item.getQuantity().intValue());
                product.setStockQuantity(newStock);
                productRepository.save(product);
                log.info("Decremented stock for product {} to {}", productId, newStock);
            });
        }
    }
}
