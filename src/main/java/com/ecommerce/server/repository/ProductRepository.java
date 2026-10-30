package com.ecommerce.server.repository;

import com.ecommerce.server.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByNameIgnoreCaseAndSeller_Id(String name, Long sellerId);

    boolean existsByNameIgnoreCaseAndSeller_IdAndIdNot(String name, Long sellerId, Long id);
}
