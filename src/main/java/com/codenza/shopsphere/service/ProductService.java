package com.codenza.shopsphere.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.PageResponse;
import com.codenza.shopsphere.dto.ProductRequest;
import com.codenza.shopsphere.dto.ProductResponse;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.exception.UnauthorizedActionException;
import com.codenza.shopsphere.mapper.ProductMapper;
import com.codenza.shopsphere.repository.CategoryRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.repository.spec.ProductSpecifications;
import com.codenza.shopsphere.security.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.categoryId()));

        User seller = currentUserProvider.getCurrentUser();

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
        product.setSeller(seller);

        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(Long categoryId, String keyword,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 Pageable pageable) {
        Specification<Product> spec = Specification
                .allOf(ProductSpecifications.hasCategoryId(categoryId))
                .and(ProductSpecifications.nameContains(keyword))
                .and(ProductSpecifications.priceGreaterOrEqual(minPrice))
                .and(ProductSpecifications.priceLessOrEqual(maxPrice));

        Page<Product> page = productRepository.findAll(spec, pageable);
        Page<ProductResponse> mapped = page.map(productMapper::toResponse);

        return new PageResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages(),
                mapped.isLast());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        assertOwnedByCurrentUser(product);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);

        return productMapper.toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        assertOwnedByCurrentUser(product);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void assertOwnedByCurrentUser(Product product) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = product.getSeller().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You do not have permission to modify this product");
        }
    }
}