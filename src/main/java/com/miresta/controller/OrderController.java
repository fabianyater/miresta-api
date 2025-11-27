package com.miresta.controller;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.dto.OrderDetailsResponse;
import com.miresta.dto.OrdersResponse;
import com.miresta.services.IOrderService;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final IOrderService orderService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest request) {
        orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrdersResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getOrders());
    }
    
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDetailsResponse> getOrderDetail(@PathVariable Long orderId) {
        OrderDetailsResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }
}
