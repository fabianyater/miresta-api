package com.miresta.table;

import java.time.Instant;

public record SalonLayoutResponse(Long salonId, Instant savedAt, String savedBy, int tableCount) {
}
