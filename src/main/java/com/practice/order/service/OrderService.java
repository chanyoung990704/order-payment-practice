package com.practice.order.service;

import com.practice.order.domain.Money;
import com.practice.order.domain.Order;
import com.practice.order.dto.OrderCreateRequest;
import com.practice.order.dto.OrderResponse;
import com.practice.order.exception.ErrorCode;
import com.practice.order.exception.OrderException;
import com.practice.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        orderRepository.findById(request.orderId()).ifPresent(existing -> {
            throw new OrderException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        });

        Order order = Order.create(
                request.orderId(),
                request.userId(),
                request.productName(),
                new Money(request.amount()),
                "order:" + request.orderId()
        );
        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }
}
