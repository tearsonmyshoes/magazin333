package com.ecommerce.server.controller;

import jakarta.validation.Valid;

import com.ecommerce.server.dto.product.CreateProductRequest;
import com.ecommerce.server.dto.product.ProductFilterRequest;
import com.ecommerce.server.dto.product.ProductResponse;
import com.ecommerce.server.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.findAll();
    }

    @PostMapping("/filter")
    public List<ProductResponse> filterProducts(
            @Valid @RequestBody(required = false) ProductFilterRequest request
    ) {
        return productService.filter(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }
}
