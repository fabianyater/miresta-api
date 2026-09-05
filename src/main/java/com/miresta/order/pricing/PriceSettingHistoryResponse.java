package com.miresta.order.pricing;

import java.time.Instant;

public record PriceSettingHistoryResponse(
        Long id,
        long previousAmount,
        long newAmount,
        String previousLabel,
        String newLabel,
        boolean previousConfirmed,
        boolean newConfirmed,
        String changedBy,
        Instant changedAt) {
}
