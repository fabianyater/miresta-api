package com.miresta.order;

import java.time.LocalDate;
import java.util.List;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);

    List<OrdersResponse> getOrders(String status, Long customerId);

    List<OrdersResponse> getOrderHistory(LocalDate date);

    OrderDetailsResponse getOrderDetail(Long orderId);

    OrderDetailsResponse getPendingOrderDetail(Long tableId);

    /**
     * @return el resultado del cobro (con el cambio a devolver) si el pedido se
     * completó pagando; {@code null} si se canceló o quedó como cuenta abierta.
     */
    PaymentResultResponse updateOrderStatus(Long orderId, String status, List<PaymentLine> payments);

    PaymentResultResponse payOrder(Long orderId, List<PaymentLine> payments);

    /**
     * Splits off every plato in this (still-pending) order that belongs to the given
     * customer into its own open tab (a new COMPLETED, unpaid order billed to them),
     * leaving the rest of the ticket as-is. Returns the original order's own updated
     * details — still PENDING with a smaller total if platos remain, or CANCELLED
     * (nothing left to resolve) if everything just moved to the new tab.
     */
    OrderDetailsResponse fiarCliente(Long orderId, Long customerId);

    SettleTabResponse settleCustomerTab(Long customerId, Long paymentTypeId);

    List<CustomerBalanceResponse> getCustomerBalances();

    List<PaymentTotalResponse> getPaymentTotals(LocalDate date);

    DailyReportResponse getDailyReport(LocalDate date);
}
