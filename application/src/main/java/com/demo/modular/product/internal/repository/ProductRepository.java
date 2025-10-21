package com.demo.modular.product.internal.repository;

import com.demo.modular.product.internal.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.stock.value > :stock")
    List<Product> findByStockGreaterThan(@Param("stock") Integer stock);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name.value) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);
}

