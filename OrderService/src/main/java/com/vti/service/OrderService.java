package com.vti.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vti.client.ProductGateway;
import com.vti.client.dto.ProductClientDto;
import com.vti.dto.OrderDto;
import com.vti.dto.OrderItemDto;
import com.vti.entity.Order;
import com.vti.entity.OrderItems;
import com.vti.entity.enums.OrderStatus;
import com.vti.form.OrderForm;
import com.vti.form.OrderFormUpdate;
import com.vti.form.OrderItemForm;
import com.vti.repository.IOrderRepository;

@Service
public class OrderService implements IOrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private IOrderRepository orderRepository;

    @Autowired
    private ProductGateway productGateway;

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems() == null ? List.of() :
                order.getItems().stream()
                        .map(i -> OrderItemDto.builder()
                                .id(i.getId())
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .subtotal(i.getSubtotal())
                                .build())
                        .toList();

        return OrderDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public OrderDto createOrder(OrderForm form) {
        Order order = Order.builder()
                .userId(form.getUserId())
                .shippingAddress(form.getShippingAddress())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItems> orderItems = new ArrayList<>();
        // theo dõi các item đã trừ kho thành công để hoàn kho (compensation) nếu lỗi giữa chừng
        List<OrderItemForm> deducted = new ArrayList<>();

        try {
            for (OrderItemForm itemForm : form.getItems()) {
                ProductClientDto product = productGateway.fetchProduct(itemForm.getProductId());

                if (!"AVAILABLE".equals(product.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm '" + product.getName() + "' hiện không khả dụng");
                }
                if (product.getQuantity() < itemForm.getQuantity()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sản phẩm '" + product.getName() + "' không đủ tồn kho");
                }

                BigDecimal price = product.getPrice();
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemForm.getQuantity()));
                total = total.add(subtotal);

                orderItems.add(OrderItems.builder()
                        .order(order)
                        .productId(itemForm.getProductId())
                        .quantity(itemForm.getQuantity())
                        .price(price)
                        .subtotal(subtotal)
                        .build());

                productGateway.adjustQuantity(itemForm.getProductId(), -itemForm.getQuantity());
                deducted.add(itemForm);
            }
        } catch (RuntimeException e) {
            for (OrderItemForm done : deducted) {
                try {
                    productGateway.adjustQuantity(done.getProductId(), done.getQuantity());
                } catch (Exception compensateError) {
                    // không throw tiếp — log lại để xử lý thủ công, tránh nuốt mất lỗi gốc
                    log.error("Hoàn kho thất bại cho productId={}: {}", done.getProductId(), compensateError.getMessage(), compensateError);
                }
            }
            throw e;
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        return toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderById(Long id, Long currentUserId, String currentUserRole, boolean internalCall) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng id=" + id));
        if (!internalCall) {
            boolean isOwner = order.getUserId().equals(currentUserId);
            boolean isAdmin = "ADMIN".equals(currentUserRole);
            if (!isOwner && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem đơn hàng này");
            }
        }
        return toDto(order);
    }

    @Override
    public List<OrderDto> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public OrderDto updateOrderStatus(Long id, OrderStatus status, String currentUserRole, boolean internalCall) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng id=" + id));
        if (!internalCall) {
            boolean isAdmin = "ADMIN".equals(currentUserRole);
            if (!isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật trạng thái đơn hàng này");
            }
        }
        order.setStatus(status);
        return toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto updateOrder(Long id, OrderFormUpdate form, Long currentUserId, String currentUserRole) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng id=" + id));
        boolean isOwner = order.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentUserRole);
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật đơn hàng này");
        }
        if (form.getShippingAddress() != null) {
            order.setShippingAddress(form.getShippingAddress());
        }
        return toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void cancelOrder(Long id, Long currentUserId, String currentUserRole) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng id=" + id));

        boolean isOwner = order.getUserId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equals(currentUserRole);
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền huỷ đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể huỷ đơn khi đang PENDING");
        }

        for (OrderItems item : order.getItems()) {
            productGateway.adjustQuantity(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}