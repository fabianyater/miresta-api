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
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest request) {
        orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<List<OrdersResponse>> getOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "customerId", required = false) Long customerId
    ) {
        return ResponseEntity.ok(orderService.getOrders(status, customerId));
    }

    @GetMapping("/history")
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<List<OrdersResponse>> getOrderHistory(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getOrderHistory(date));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(@PathVariable Long orderId) {
        OrderDetailsResponse orderDetail = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @GetMapping("/pending/{tableId}")
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<OrderDetailsResponse> getPendingOrderDetails(@PathVariable Long tableId) {
        OrderDetailsResponse orderDetail = orderService.getPendingOrderDetail(tableId);
        return ResponseEntity.ok(orderDetail);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("@access.has('PEDIDOS_CREAR')")
    public ResponseEntity<PaymentResultResponse> updateOrderStatus(
            @RequestBody UpdateStatusRequest request,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.status(), request.payments()));
    }

    @GetMapping("/reports/payment-totals")
    @PreAuthorize("@access.has('PEDIDOS_REPORTES')")
    public ResponseEntity<List<PaymentTotalResponse>> getPaymentTotals(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getPaymentTotals(date));
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("@access.has('PEDIDOS_REPORTES')")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.getDailyReport(date));
    }

    @PatchMapping("/{orderId}/pay")
    @PreAuthorize("@access.has('PEDIDOS_COBRAR')")
    public ResponseEntity<PaymentResultResponse> payOrder(
            @PathVariable Long orderId, @RequestBody PayOrderRequest request) {
        return ResponseEntity.ok(orderService.payOrder(orderId, request.payments()));
    }

    @PatchMapping("/{orderId}/fiar-cliente")
    @PreAuthorize("@access.has('PEDIDOS_COBRAR')")
    public ResponseEntity<OrderDetailsResponse> fiarCliente(
            @PathVariable Long orderId, @RequestBody FiarClienteRequest request) {
        return ResponseEntity.ok(orderService.fiarCliente(orderId, request.customerId()));
    }

    @PostMapping("/customers/{customerId}/settle")
    @PreAuthorize("@access.has('PEDIDOS_COBRAR')")
    public ResponseEntity<SettleTabResponse> settleCustomerTab(
            @PathVariable Long customerId, @RequestBody SettleTabRequest request) {
        return ResponseEntity.ok(orderService.settleCustomerTab(customerId, request.paymentTypeId()));
    }

    @GetMapping("/customers/balances")
    @PreAuthorize("@access.has('PEDIDOS_COBRAR')")
    public ResponseEntity<List<CustomerBalanceResponse>> getCustomerBalances() {
        return ResponseEntity.ok(orderService.getCustomerBalances());
    }

    @GetMapping("/customers/{customerId}/payments")
    @PreAuthorize("@access.has('PEDIDOS_COBRAR')")
    public ResponseEntity<List<CustomerPaymentResponse>> getCustomerPayments(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getCustomerPayments(customerId));
    }
}
