package com.ecommerce.server.service;

import com.ecommerce.server.dto.product.CreateProductRequest;
import com.ecommerce.server.dto.product.ProductFilterRequest;
import com.ecommerce.server.dto.product.ProductResponse;
import com.ecommerce.server.dto.product.UpdateProductRequest;
import com.ecommerce.server.entity.Category;
import com.ecommerce.server.entity.Product;
import com.ecommerce.server.entity.User;
import com.ecommerce.server.repository.CategoryRepository;
import com.ecommerce.server.repository.ProductRepository;
import com.ecommerce.server.repository.UserRepository;

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
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
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
    public ProductResponse findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> filter(ProductFilterRequest request) {
        ProductFilterRequest filter = request == null
                ? new ProductFilterRequest(null, null, null, null, null, null, null)
                : request;

        validateFilter(filter);

        return productRepository
                .findAll(buildSpecification(filter), Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request, String sellerEmail) {
        User seller = getUserByEmail(sellerEmail);
        String name = normalizeName(request.name());

        if (productRepository.existsByNameIgnoreCaseAndSeller_Id(name, seller.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a product with this name"
            );
        }

        Product product = new Product(
                name,
                normalizeDescription(request.description()),
                request.price(),
                request.stock() == null ? 0 : request.stock(),
                resolveCategory(request.categoryId()),
                seller
        );

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = getProduct(id);
        Long sellerId = product.getSeller() == null ? null : product.getSeller().getId();

        if (request.name() != null) {
            String name = normalizeName(request.name());

            if (sellerId != null && productRepository
                    .existsByNameIgnoreCaseAndSeller_IdAndIdNot(name, sellerId, product.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This seller already has a product with this name"
                );
            }

            product.setName(name);
        }

        if (request.description() != null) {
            product.setDescription(normalizeDescription(request.description()));
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.stock() != null) {
            product.setStock(request.stock());
        }

        if (request.categoryId() != null) {
            product.setCategory(resolveCategory(request.categoryId()));
        }

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse patch(Long id, UpdateProductRequest request) {
        if (request == null
                || (request.name() == null
                && request.description() == null
                && request.price() == null
                && request.stock() == null
                && request.categoryId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one field must be provided"
            );
        }

        return update(id, request);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }

    private User getUserByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found"
                ));
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product name must not be blank"
            );
        }

        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
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

            if (filter.sellerId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("seller").get("id"),
                        filter.sellerId()
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
        Long sellerId = product.getSeller() == null ? null : product.getSeller().getId();
        String sellerEmail = product.getSeller() == null ? null : product.getSeller().getEmail();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                categoryId,
                categoryName,
                sellerId,
                sellerEmail,
                product.getCreatedAt()
        );
    }
}
