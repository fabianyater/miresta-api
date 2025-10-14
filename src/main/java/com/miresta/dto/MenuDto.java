package com.miresta.dto;

import java.util.List;

public record MenuDto(Long id,
                      String date,
                      String type,
                      List<ItemDto> items) {
}
