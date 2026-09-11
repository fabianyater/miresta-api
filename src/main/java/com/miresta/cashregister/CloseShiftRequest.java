package com.miresta.cashregister;

public record CloseShiftRequest(long countedCash, String notes) {
}
