package com.vti.form;

import com.vti.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class PaymentForm {

    @NotNull(message = "Mã đơn hàng không được để trống")
    private Long orderId;

    private Long userId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod method;
}