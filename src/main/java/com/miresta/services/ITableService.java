package com.miresta.services;

import com.miresta.dto.response.TableEntityDto;
import com.miresta.entity.DiningTable;

import java.util.List;

public interface ITableService {
    List<TableEntityDto> getTables();
    Integer getTablesCounter(String status);
    void updateTableStatus(DiningTable diningTable, String status);
}
