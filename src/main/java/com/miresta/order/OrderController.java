package com.miresta.order;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest request) {
        orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<OrdersResponse>> getOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "customerId", required = false) Long customerId
    ) {
        return ResponseEntity.ok(orderService.getOrders(status, customerId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<OrdersResponse>> getOrderHistory(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getOrderHistory(date));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long orderId) {
        OrderDetailsResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping("/pending/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<OrderDetailsResponse> getPendingOrderDetails(@PathVariable Long tableId) {
        OrderDetailsResponse orderDetail = orderService.getPendingOrderDetail(tableId);
        return ResponseEntity.ok(orderDetail);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<Void> updateOrderStatus(
            @RequestBody UpdateStatusRequest request,
            @PathVariable Long orderId) {
        orderService.updateOrderStatus(orderId, request.status(), request.payments());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/reports/payment-totals")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<List<PaymentTotalResponse>> getPaymentTotals(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getPaymentTotals(date));
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getDailyReport(date));
    }

    @PatchMapping("/{orderId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<Void> payOrder(@PathVariable Long orderId, @RequestBody PayOrderRequest request) {
        orderService.payOrder(orderId, request.payments());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{orderId}/fiar-cliente")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<OrderDetailsResponse> fiarCliente(
            @PathVariable Long orderId, @RequestBody FiarClienteRequest request) {
        return ResponseEntity.ok(orderService.fiarCliente(orderId, request.customerId()));
    }

    @PostMapping("/customers/{customerId}/settle")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<SettleTabResponse> settleCustomerTab(
            @PathVariable Long customerId, @RequestBody SettleTabRequest request) {
        return ResponseEntity.ok(orderService.settleCustomerTab(customerId, request.paymentTypeId()));
    }

    @GetMapping("/customers/balances")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<CustomerBalanceResponse>> getCustomerBalances() {
        return ResponseEntity.ok(orderService.getCustomerBalances());
    }
}
