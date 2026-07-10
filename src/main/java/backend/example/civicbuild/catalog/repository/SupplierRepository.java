package backend.example.civicbuild.catalog.repository;

import backend.example.civicbuild.catalog.entity.Supplier;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    @Query("""
            SELECT s FROM Supplier s
            WHERE (:namePattern IS NULL OR LOWER(s.name) LIKE :namePattern)
              AND (:category IS NULL OR s.category.id = :category)
            """)
    Page<Supplier> search(
            @Param("namePattern") String namePattern,
            @Param("category") String category,
            Pageable pageable);
}
