package com.miresta.controller;

import com.miresta.dto.request.CreateOrderRequest;
import com.miresta.dto.request.UpdateStatusRequest;
import com.miresta.dto.response.OrderDetailsResponse;
import com.miresta.dto.response.OrdersResponse;
import com.miresta.services.IOrderService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<OrdersResponse>> getOrders(
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(orderService.getOrders(status));
    }
    
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long orderId) {
        OrderDetailsResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping("/pending/{tableId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDetailsResponse> getPendingOrderDetails(@PathVariable Long tableId) {
        OrderDetailsResponse orderDetail = orderService.getPendingOrderDetail(tableId);
        return ResponseEntity.ok(orderDetail);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateOrderStatus(
            @RequestBody UpdateStatusRequest request,
            @PathVariable Long orderId) {
        orderService.updateOrderStatus(orderId, request.status());

        return ResponseEntity.ok().build();
    }
}
