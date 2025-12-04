package com.bidket.product;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductServiceApplication {
    @PostConstruct
    public void checkDbUrl() {
        System.out.println(">>> JDBC URL = " + System.getProperty("spring.datasource.url"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}