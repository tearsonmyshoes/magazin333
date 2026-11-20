package com.ecommerce.server.service;

import com.ecommerce.server.dto.brand.BrandRequest;
import com.ecommerce.server.dto.brand.BrandResponse;
import com.ecommerce.server.dto.brand.UpdateBrandRequest;
import com.ecommerce.server.entity.Brand;
import com.ecommerce.server.repository.BrandRepository;
import com.ecommerce.server.repository.ProductRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BrandService {
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandService(
            BrandRepository brandRepository,
            ProductRepository productRepository
    ) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> findAll(String name) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");

        List<Brand> brands = name == null || name.isBlank()
                ? brandRepository.findAll(sort)
                : brandRepository.findByNameContainingIgnoreCase(name.trim(), sort);

        return brands
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse findById(Long id) {
        return toResponse(getBrand(id));
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String name = normalizeName(request.name());

        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A brand with this name already exists"
            );
        }

        return toResponse(brandRepository.save(new Brand(name)));
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        return patch(id, new UpdateBrandRequest(request.name()));
    }

    @Transactional
    public BrandResponse patch(Long id, UpdateBrandRequest request) {
        if (request == null || request.name() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one field must be provided"
            );
        }

        Brand brand = getBrand(id);
        String name = normalizeName(request.name());

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A brand with this name already exists"
            );
        }

        brand.setName(name);
        return toResponse(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = getBrand(id);

        if (productRepository.existsByBrand_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Brand cannot be deleted while products use it"
            );
        }

        brandRepository.delete(brand);
    }

    private Brand getBrand(Long id) {
        return brandRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Brand not found"
                ));
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Brand name must not be blank"
            );
        }

        return normalized;
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName());
    }
}
