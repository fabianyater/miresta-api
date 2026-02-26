package com.miresta.services.impl;

import com.miresta.dto.response.TableEntityDto;
import com.miresta.dto.response.TableSummaryResponse;
import com.miresta.entity.DiningTable;
import com.miresta.entity.DiningTableStatus;
import com.miresta.repository.DiningTableStatusRepository;
import com.miresta.repository.TableEntityRepository;
import com.miresta.services.ITableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TableServiceImpl implements ITableService {
    private final TableEntityRepository tableEntityRepository;
    private final DiningTableStatusRepository diningTableStatusRepository;

    @Override
    public TableSummaryResponse getTablesInfo() {
        List<TableEntityDto> tables = tableEntityRepository.findAllAsDto().stream()
                .sorted(Comparator.comparing(TableEntityDto::number))
                .toList();
        Object result = tableEntityRepository.countAllStatuses();

        long free = 0L;
        long inUse = 0L;

        if (result instanceof Object[] row) {
            free = (row[0] != null) ? ((Number) row[0]).longValue() : 0L;
            inUse = (row[1] != null) ? ((Number) row[1]).longValue() : 0L;
        }

        return new TableSummaryResponse(tables, free, inUse);
    }

    @Transactional
    @Override
    public void updateTableStatus(DiningTable diningTable, String status) {
        DiningTableStatus diningTableStatus = diningTableStatusRepository.findByName(status);

        diningTable.setStatus(diningTableStatus);

        tableEntityRepository.save(diningTable);
    }
}
