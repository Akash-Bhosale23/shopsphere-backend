package com.codenza.shopsphere.mapper;

import org.springframework.stereotype.Component;

import com.codenza.shopsphere.dto.CategorySummary;
import com.codenza.shopsphere.dto.ProductResponse;
import com.codenza.shopsphere.dto.SellerSummary;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        User seller = product.getSeller();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                new CategorySummary(category.getId(), category.getName()),
                new SellerSummary(seller.getId(), seller.getName()));
    }
}