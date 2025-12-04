package com.miresta.dto.response;

import java.util.List;

public record MenuResponse(Long id,
                           String date,
                           String type,
                           List<ItemResponse> items) {
}
