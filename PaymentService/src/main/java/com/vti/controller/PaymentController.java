package com.vti.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.vti.client.dto.PaymentDto;
import com.vti.form.PaymentForm;
import com.vti.form.PaymentFormUpdate;
import com.vti.service.IPaymentService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private IPaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> create(
            @RequestBody @Valid PaymentForm form,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            form.setUserId(userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentDto>> getByOrderId(
        @PathVariable Long orderId,
        @RequestHeader (value = "X-User-Id", required = false) Long userId,
        @RequestHeader (value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId, userId, userRole));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PaymentDto> updateStatus(
        @PathVariable Long id, 
        @RequestBody @Valid PaymentFormUpdate form,
        @RequestHeader (value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, form, userRole));
    }
}