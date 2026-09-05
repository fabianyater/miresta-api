package com.miresta.order;

import com.miresta.shared.Money;

import java.util.List;

/**
 * Operational snapshot of one day, scoped by order.createdAt (when the ticket was
 * opened) — not by paidAt, which {@link PaymentTotalResponse} already covers for
 * cash-basis totals by payment method. Cancelled orders are excluded from every
 * count/total here except {@code cancelledOrders} itself.
 */
public record DailyReportResponse(
        long totalOrders,
        long cancelledOrders,
        Money totalSales,
        long registeredCustomersOrdered,
        long openTabsCount,
        Money openTabsTotal,
        long additionsCount,
        List<MealTypeSummary> mealTypeCounts,
        List<FulfillmentSummary> fulfillmentCounts,
        List<HourlyCount> hourlyCounts) {

    public record MealTypeSummary(String mealType, long orderItemCount, Money total) {
    }

    public record FulfillmentSummary(String type, long orderItemCount, Money total) {
    }

    public record HourlyCount(int hour, long orderCount) {
    }
}
