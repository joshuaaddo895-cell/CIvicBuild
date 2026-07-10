package backend.example.civicbuild.catalog.repository;

import backend.example.civicbuild.catalog.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:namePattern IS NULL OR LOWER(p.name) LIKE :namePattern)
              AND (:category IS NULL OR p.category = :category)
              AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
              AND (:agencyId IS NULL OR p.agency.id = :agencyId)
            """)
    Page<Product> search(
            @Param("namePattern") String namePattern,
            @Param("category") String category,
            @Param("supplierId") UUID supplierId,
            @Param("agencyId") UUID agencyId,
            Pageable pageable);

    Page<Product> findByAgencyIdAndActiveTrue(UUID agencyId, Pageable pageable);

    Optional<Product> findByIdAndAgencyId(UUID id, UUID agencyId);
}
