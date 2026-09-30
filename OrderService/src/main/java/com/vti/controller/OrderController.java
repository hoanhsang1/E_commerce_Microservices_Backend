package com.vti.controller;

import com.vti.annotation.InternalApi;
import com.vti.dto.OrderDto;
import com.vti.entity.enums.OrderStatus;
import com.vti.form.OrderForm;
import com.vti.form.OrderFormUpdate;
import com.vti.service.IOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private IOrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> create(
            @RequestBody @Valid OrderForm form,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            form.setUserId(userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        boolean internalCall = apiKey != null && !apiKey.isBlank();
        return ResponseEntity.ok(orderService.getOrderById(id, userId, userRole, internalCall));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        boolean internalCall = apiKey != null && !apiKey.isBlank();
        OrderStatus status = OrderStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status, userRole, internalCall));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        boolean isOwner = currentUserId != null && currentUserId.equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách đơn hàng của người khác");
        }
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(
            @PathVariable Long id, 
            @RequestBody @Valid OrderFormUpdate form,
            @RequestHeader (value = "X-User-Id", required = false) Long userId,
            @RequestHeader (value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(orderService.updateOrder(id, form, userId, userRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @RequestHeader (value = "X-User-Id", required = false) Long userId,
            @RequestHeader (value = "X-User-Role", required = false) String userRole) {
        orderService.cancelOrder(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }
}