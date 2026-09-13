package com.miresta.table;

import java.util.List;

public interface ITableService {
    TableSummaryResponse getTablesInfo();

    void updateTableStatus(DiningTable diningTable, String status);

    TableEntityDto createTable(TableRequest request);

    TableEntityDto renameTable(Long id, TableRequest request);

    TableEntityDto updateTablePosition(Long id, TablePositionRequest request);

    void deleteTable(Long id);

    /** Une las mesas de `tableIds` a `primaryId` — todas deben estar libres y sin
     * unirse ya a otro grupo. Mientras dure la unión, el pedido/cuenta corre por la
     * principal; las demás no generan uno propio. */
    void mergeTables(Long primaryId, List<Long> tableIds);

    /** Disuelve el grupo unido a `primaryId`, si tiene — no falla si no tiene ninguno. */
    void unmergeTables(Long primaryId);
}
