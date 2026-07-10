package backend.example.civicbuild.catalog.repository;

import backend.example.civicbuild.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {}
