package com.miresta.table;

public interface ITableService {
    TableSummaryResponse getTablesInfo();

    void updateTableStatus(DiningTable diningTable, String status);

    TableEntityDto createTable(TableRequest request);

    TableEntityDto renameTable(Long id, TableRequest request);

    TableEntityDto updateTablePosition(Long id, TablePositionRequest request);

    void deleteTable(Long id);
}
