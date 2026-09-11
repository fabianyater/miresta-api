package com.miresta.cashregister;

import com.miresta.shared.Money;

import java.time.Instant;

public record CashMovementResponse(
        Long id, String type, Money amount, String reason, Instant createdAt, String createdBy) {
}
