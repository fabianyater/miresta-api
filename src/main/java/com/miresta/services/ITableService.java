package com.miresta.services;

import com.miresta.dto.response.TableEntityDto;
import com.miresta.dto.response.TableSummaryResponse;
import com.miresta.entity.DiningTable;

import java.util.List;

public interface ITableService {
    TableSummaryResponse getTablesInfo();
    void updateTableStatus(DiningTable diningTable, String status);
}
