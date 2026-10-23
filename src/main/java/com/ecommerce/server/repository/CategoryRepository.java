package com.ecommerce.server.repository;

import com.ecommerce.server.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
