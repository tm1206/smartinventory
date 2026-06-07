package com.smartinventory.repository;

import com.smartinventory.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    List<Product> findByCategory(String category);
    List<Product> findByQuantityLessThan(Integer threshold);

    @Query("SELECT p FROM Product p WHERE " +
           "(:name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:category = '' OR p.category = :category)")
    Page<Product> searchProducts(@Param("name") String name,
                                  @Param("category") String category,
                                  Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();
}
