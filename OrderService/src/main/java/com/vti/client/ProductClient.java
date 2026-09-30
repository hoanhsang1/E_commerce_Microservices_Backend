package com.vti.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.vti.client.dto.ProductClientDto;

// url cố định dùng Docker container name, bỏ qua Eureka LoadBalancer
@FeignClient(name = "ProductService", url = "${product.service.url:http://product-service:8082}", configuration = com.vti.client.config.FeignInternalAuthConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductClientDto getProductById(@PathVariable("id") Long id);

    @PutMapping("/api/v1/products/{id}/quantity")
    ProductClientDto updateQuantity(@PathVariable("id") Long id, @RequestBody Integer delta);
}