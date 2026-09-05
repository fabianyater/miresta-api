package com.miresta.table;

import java.util.List;

public record TableSummaryResponse(List<TableEntityDto> tables, Long freeTables, Long inUseTables) {
}
