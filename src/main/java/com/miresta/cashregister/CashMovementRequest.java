package com.miresta.cashregister;

public record CashMovementRequest(String type, long amount, String reason) {
}
