package com.demo.modular.product.repository;

import com.demo.modular.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByStockGreaterThan(Integer stock);
    
    List<Product> findByNameContainingIgnoreCase(String name);
}

