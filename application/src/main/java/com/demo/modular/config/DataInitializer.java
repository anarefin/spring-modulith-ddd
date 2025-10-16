package com.demo.modular.config;

import com.demo.modular.product.domain.Product;
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
        return _ -> {
            log.info("Initializing sample data...");
            
            // Create sample products
            Product laptop = new Product();
            laptop.setName("Dell XPS 15 Laptop");
            laptop.setDescription("High-performance laptop with 16GB RAM and 512GB SSD");
            laptop.setPrice(new BigDecimal("1299.99"));
            laptop.setStock(10);
            productService.createProduct(laptop);
            
            Product mouse = new Product();
            mouse.setName("Logitech MX Master 3");
            mouse.setDescription("Advanced wireless mouse for productivity");
            mouse.setPrice(new BigDecimal("99.99"));
            mouse.setStock(50);
            productService.createProduct(mouse);
            
            Product keyboard = new Product();
            keyboard.setName("Keychron K2 Mechanical Keyboard");
            keyboard.setDescription("Wireless mechanical keyboard with RGB backlight");
            keyboard.setPrice(new BigDecimal("79.99"));
            keyboard.setStock(30);
            productService.createProduct(keyboard);
            
            Product monitor = new Product();
            monitor.setName("LG 27 4K UHD Monitor");
            monitor.setDescription("27-inch 4K monitor with USB-C connectivity");
            monitor.setPrice(new BigDecimal("399.99"));
            monitor.setStock(15);
            productService.createProduct(monitor);
            
            Product headphones = new Product();
            headphones.setName("Sony WH-1000XM5 Headphones");
            headphones.setDescription("Premium noise-cancelling wireless headphones");
            headphones.setPrice(new BigDecimal("349.99"));
            headphones.setStock(25);
            productService.createProduct(headphones);
            
            log.info("Sample data initialized successfully!");
        };
    }
}

