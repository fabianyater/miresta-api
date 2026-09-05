package com.miresta.order.pricing;

/** All fields optional — only the ones present are applied. */
public record UpdatePriceSettingRequest(Long amount, String label, Boolean confirmed) {
}
