package com.miresta.services;

import com.miresta.dto.TableEntityDto;
import com.miresta.entity.DiningTable;

import java.util.List;

public interface ITableService {
    List<TableEntityDto> getTables();
    Integer getFreeTablesCount();
    Integer getOccupiedTablesCount();
    void updateTableStatus(DiningTable diningTable, String status);
}
