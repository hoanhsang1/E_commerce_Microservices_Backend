package com.vti.controller;

import com.vti.annotation.InternalApi;
import com.vti.dto.ProductDto;
import com.vti.entity.enums.ProductStatus;
import com.vti.form.ProductForm;
import com.vti.service.IProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private IProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> create(@RequestBody @Valid ProductForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(form));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(productService.searchProducts(category, status, name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> update(
            @PathVariable Long id,
            @RequestBody @Valid ProductForm form) {
        return ResponseEntity.ok(productService.updateProduct(id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // OrderService sẽ gọi API này khi làm phần liên service (delta âm khi trừ kho lúc đặt hàng)
    @RequestMapping(value = "/{id}/quantity", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @InternalApi
    public ResponseEntity<ProductDto> updateQuantity(
            @PathVariable Long id,
            @RequestBody Integer delta) {
        return ResponseEntity.ok(productService.updateQuantity(id, delta));
    }
}