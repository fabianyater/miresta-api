package com.miresta.controller;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.services.impl.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderServiceImpl orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest orderRequest) {
        log.info("Creating order: {}", orderRequest);
        orderService.createOrder(orderRequest);

        return ResponseEntity.ok().build();
    }
}
