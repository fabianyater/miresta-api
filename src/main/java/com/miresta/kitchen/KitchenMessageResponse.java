package com.miresta.kitchen;

import java.time.Instant;

public record KitchenMessageResponse(Long id, String text, String sentBy, Instant createdAt) {
}
