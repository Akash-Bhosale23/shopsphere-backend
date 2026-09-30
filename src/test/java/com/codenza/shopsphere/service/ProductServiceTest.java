package com.codenza.shopsphere.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codenza.shopsphere.dto.ProductRequest;
import com.codenza.shopsphere.dto.ProductResponse;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.exception.UnauthorizedActionException;
import com.codenza.shopsphere.mapper.ProductMapper;
import com.codenza.shopsphere.repository.CategoryRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.security.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductMapper productMapper;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@InjectMocks
	private ProductService productService;

	private Category category;
	private Product product;
	private User seller;
	private User admin;

	@BeforeEach
	void setUp() {

		category = new Category();
		category.setId(1L);
		category.setName("Electronics");

		seller = new User();
		seller.setId(10L);
		seller.setName("Seller");
		seller.setEmail("seller@test.com");
		seller.setRole(Role.SELLER);

		admin = new User();
		admin.setId(20L);
		admin.setName("Admin");
		admin.setEmail("admin@test.com");
		admin.setRole(Role.ADMIN);

		product = new Product();
		product.setId(100L);
		product.setName("Laptop");
		product.setDescription("Gaming Laptop");
		product.setPrice(new BigDecimal("50000"));
		product.setStockQuantity(10);
		product.setCategory(category);
		product.setSeller(seller);
	}

	@Test
	void create_shouldSaveProduct_whenCategoryExists() {

		ProductRequest request = new ProductRequest("Laptop", "Gaming Laptop", new BigDecimal("50000"), 10, 1L);

		ProductResponse expectedResponse = mock(ProductResponse.class);

		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		when(currentUserProvider.getCurrentUser()).thenReturn(seller);

		when(productRepository.save(any(Product.class))).thenReturn(product);

		when(productMapper.toResponse(product)).thenReturn(expectedResponse);

		ProductResponse result = productService.create(request);

		assertEquals(expectedResponse, result);

		verify(categoryRepository).findById(1L);

		verify(currentUserProvider).getCurrentUser();

		verify(productRepository).save(any(Product.class));

		verify(productMapper).toResponse(product);
	}

	@Test
	void create_shouldThrowResourceNotFoundException_whenCategoryDoesNotExist() {

		ProductRequest request = new ProductRequest("Laptop", "Gaming Laptop", new BigDecimal("50000"), 10, 99L);

		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> productService.create(request));

		assertEquals("Category not found with id: 99", exception.getMessage());

		verify(categoryRepository).findById(99L);

		verify(currentUserProvider, never()).getCurrentUser();

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void update_shouldThrowUnauthorizedActionException_whenUserIsNotOwnerOrAdmin() {

		ProductRequest request = new ProductRequest("Updated Laptop", "Updated Gaming Laptop", new BigDecimal("60000"),
				20, 1L);

		User otherUser = new User();
		otherUser.setId(30L);
		otherUser.setName("Other User");
		otherUser.setEmail("other@test.com");
		otherUser.setRole(Role.SELLER);

		when(productRepository.findById(100L)).thenReturn(Optional.of(product));

		when(currentUserProvider.getCurrentUser()).thenReturn(otherUser);

		UnauthorizedActionException exception = assertThrows(UnauthorizedActionException.class,
				() -> productService.update(100L, request));

		assertEquals("You do not have permission to modify this product", exception.getMessage());

		verify(productRepository).findById(100L);

		verify(currentUserProvider).getCurrentUser();

		verify(categoryRepository, never()).findById(anyLong());

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void update_shouldSucceed_whenUserIsTheOwner() {

		ProductRequest request = new ProductRequest("Updated Laptop", "Updated Gaming Laptop", new BigDecimal("60000"),
				20, 1L);

		ProductResponse expectedResponse = mock(ProductResponse.class);

		when(productRepository.findById(100L)).thenReturn(Optional.of(product));

		when(currentUserProvider.getCurrentUser()).thenReturn(seller);

		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		when(productMapper.toResponse(product)).thenReturn(expectedResponse);

		ProductResponse result = productService.update(100L, request);

		assertEquals(expectedResponse, result);

		assertEquals("Updated Laptop", product.getName());

		assertEquals("Updated Gaming Laptop", product.getDescription());

		assertEquals(new BigDecimal("60000"), product.getPrice());

		assertEquals(20, product.getStockQuantity());

		assertEquals(category, product.getCategory());

		verify(productRepository).findById(100L);

		verify(currentUserProvider).getCurrentUser();

		verify(categoryRepository).findById(1L);

		verify(productMapper).toResponse(product);

		verify(productRepository, never()).save(any(Product.class));
	}

	@Test
	void delete_shouldSucceed_whenUserIsAdmin_evenIfNotOwner() {

		when(productRepository.findById(100L)).thenReturn(Optional.of(product));

		when(currentUserProvider.getCurrentUser()).thenReturn(admin);

		productService.delete(100L);

		verify(productRepository).findById(100L);

		verify(currentUserProvider).getCurrentUser();

		verify(productRepository).delete(product);
	}
	
	@Test
	void create_shouldSaveProduct_withCorrectFieldsFromRequestAndCurrentUser() {

	    ProductRequest request = new ProductRequest("Laptop", "Gaming Laptop", new BigDecimal("50000"), 10, 1L);
	    ProductResponse expectedResponse = mock(ProductResponse.class);

	    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
	    when(currentUserProvider.getCurrentUser()).thenReturn(seller);
	    when(productRepository.save(any(Product.class))).thenReturn(product);
	    when(productMapper.toResponse(product)).thenReturn(expectedResponse);

	    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

	    productService.create(request);

	    verify(productRepository).save(productCaptor.capture());
	    Product savedProduct = productCaptor.getValue();

	    assertEquals("Laptop", savedProduct.getName());
	    assertEquals(new BigDecimal("50000"), savedProduct.getPrice());
	    assertEquals(category, savedProduct.getCategory());
	    assertEquals(seller, savedProduct.getSeller());
	}
}
