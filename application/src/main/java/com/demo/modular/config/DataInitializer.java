package com.demo.modular.config;

import com.demo.modular.product.api.dto.ProductDTO;
import com.demo.modular.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ProductService productService) {
        return args -> {
            log.info("Initializing sample data...");
            
            // Create sample products
            ProductDTO laptop = ProductDTO.builder()
                    .name("Dell XPS 15 Laptop")
                    .description("High-performance laptop with 16GB RAM and 512GB SSD")
                    .price(new BigDecimal("1299.99"))
                    .stock(10)
                    .build();
            productService.createProduct(laptop);
            
            ProductDTO mouse = ProductDTO.builder()
                    .name("Logitech MX Master 3")
                    .description("Advanced wireless mouse for productivity")
                    .price(new BigDecimal("99.99"))
                    .stock(50)
                    .build();
            productService.createProduct(mouse);
            
            ProductDTO keyboard = ProductDTO.builder()
                    .name("Keychron K2 Mechanical Keyboard")
                    .description("Wireless mechanical keyboard with RGB backlight")
                    .price(new BigDecimal("79.99"))
                    .stock(30)
                    .build();
            productService.createProduct(keyboard);
            
            ProductDTO monitor = ProductDTO.builder()
                    .name("LG 27 4K UHD Monitor")
                    .description("27-inch 4K monitor with USB-C connectivity")
                    .price(new BigDecimal("399.99"))
                    .stock(15)
                    .build();
            productService.createProduct(monitor);
            
            ProductDTO headphones = ProductDTO.builder()
                    .name("Sony WH-1000XM5 Headphones")
                    .description("Premium noise-cancelling wireless headphones")
                    .price(new BigDecimal("349.99"))
                    .stock(25)
                    .build();
            productService.createProduct(headphones);
            
            log.info("Sample data initialized successfully!");
        };
    }
}

