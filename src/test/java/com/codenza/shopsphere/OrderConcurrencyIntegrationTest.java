package com.codenza.shopsphere;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.repository.CategoryRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.repository.UserRepository;

class OrderConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(newCategory());

        User seller = new User();
        seller.setName("Seller");
        seller.setEmail("seller-" + System.nanoTime() + "@test.com");
        seller.setPassword(new BCryptPasswordEncoder().encode("Password@123"));
        seller.setRole(Role.SELLER);
        userRepository.save(seller);

        Product product = new Product();
        product.setName("Limited Edition Watch");
        product.setPrice(new BigDecimal("9999"));
        product.setStockQuantity(1); // only 1 unit exists
        product.setCategory(category);
        product.setSeller(seller);
        productId = productRepository.save(product).getId();
    }

    private Category newCategory() {
        Category category = new Category();
        category.setName("Watches-" + System.nanoTime());
        return category;
    }

    @Test
    void twoConcurrentPurchases_shouldNotBothSucceed_whenOnlyOneUnitInStock() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readySignal = new CountDownLatch(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable purchaseAttempt = () -> {
            readySignal.countDown();
            try {
                startSignal.await();

                Product product = productRepository.findById(productId).orElseThrow();
                if (product.getStockQuantity() >= 1) {
                    product.setStockQuantity(product.getStockQuantity() - 1);
                    productRepository.saveAndFlush(product);
                    successCount.incrementAndGet();
                }
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                failureCount.incrementAndGet();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(purchaseAttempt);
        executor.submit(purchaseAttempt);

        readySignal.await();       // wait until both threads have reached the starting line
        startSignal.countDown();   // release both at the same instant

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Product finalProduct = productRepository.findById(productId).orElseThrow();

        assertThat(successCount.get() + failureCount.get()).isEqualTo(2);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0);
    }
}