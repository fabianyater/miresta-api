package com.miresta.table;

import java.time.Instant;

public record SalonLayoutResponse(Long id, Long salonId, String name, Instant savedAt, String savedBy, int tableCount) {
}
