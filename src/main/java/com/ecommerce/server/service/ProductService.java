package com.ecommerce.server.service;

import com.ecommerce.server.dto.product.CreateProductRequest;
import com.ecommerce.server.dto.product.ProductFilterRequest;
import com.ecommerce.server.dto.product.ProductResponse;
import com.ecommerce.server.entity.Category;
import com.ecommerce.server.entity.Product;
import com.ecommerce.server.repository.CategoryRepository;
import com.ecommerce.server.repository.ProductRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository
                .findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> filter(ProductFilterRequest request) {
        ProductFilterRequest filter = request == null
                ? new ProductFilterRequest(null, null, null, null, null, null)
                : request;

        validateFilter(filter);

        return productRepository
                .findAll(buildSpecification(filter), Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String name = request.name().trim();

        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with this name already exists"
            );
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository
                    .findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category not found"
                    ));
        }

        Product product = new Product(
                name,
                request.description(),
                request.price(),
                request.stock() == null ? 0 : request.stock(),
                category
        );

        return toResponse(productRepository.save(product));
    }

    private void validateFilter(ProductFilterRequest filter) {
        if (filter.minPrice() != null
                && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minPrice must be less than or equal to maxPrice"
            );
        }

        if (filter.minStock() != null
                && filter.maxStock() != null
                && filter.minStock() > filter.maxStock()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minStock must be less than or equal to maxStock"
            );
        }
    }

    private Specification<Product> buildSpecification(ProductFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.name() != null && !filter.name().isBlank()) {
                String pattern = "%" + filter.name().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        pattern
                ));
            }

            if (filter.categoryId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category").get("id"),
                        filter.categoryId()
                ));
            }

            if (filter.minPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        filter.minPrice()
                ));
            }

            if (filter.maxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        filter.maxPrice()
                ));
            }

            if (filter.minStock() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("stock"),
                        filter.minStock()
                ));
            }

            if (filter.maxStock() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("stock"),
                        filter.maxStock()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ProductResponse toResponse(Product product) {
        Long categoryId = product.getCategory() == null ? null : product.getCategory().getId();
        String categoryName = product.getCategory() == null ? null : product.getCategory().getName();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                categoryId,
                categoryName,
                product.getCreatedAt()
        );
    }
}
