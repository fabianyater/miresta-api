package com.miresta.dto.response;

import java.util.List;

public record TableSummaryResponse(List<TableEntityDto> tables, Long freeTables, Long inUseTables) {
}
