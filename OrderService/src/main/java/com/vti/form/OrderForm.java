package com.vti.form;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderForm {
    private Long userId;
    private String shippingAddress;
    @NotEmpty(message = "Order must contain at least one item")
    @Valid 
    private List<OrderItemForm> items;
}