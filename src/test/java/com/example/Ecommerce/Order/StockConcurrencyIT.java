package com.example.Ecommerce.Order;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.Ecommerce.AbstractIntegrationTest;
import com.example.Ecommerce.Inventory.StockService;
import com.example.Ecommerce.Product.Product;
import com.example.Ecommerce.Product.ProductRepository;

import static org.assertj.core.api.Assertions.assertThat;

class StockConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockService stockService;

    @Test
    void containerWiringWorks() {
        Product product = new Product();
        product.setProductName("Test Widget");
        product.setProductPrice(100);
        product.setStock(10);
        Product saved = productRepository.save(product);

        Product fetched = productRepository.findById(saved.getProductId()).orElseThrow();

        assertThat(fetched.getStock()).isEqualTo(10);
    }

    @Test
    void concurrentStockReservationDoesNotOversell() throws InterruptedException {
        Product product = new Product();
        product.setProductName("Concurrent Widget");
        product.setProductPrice(50);
        product.setStock(10);
        Product saved = productRepository.saveAndFlush(product);
        Long productId = saved.getProductId();

        int threads = 20;
        int quantityPerRequest = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    stockService.reserveStock(productId, quantityPerRequest);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        Product after = productRepository.findById(productId).orElseThrow();
        assertThat(after.getStock()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(10);
    }
}