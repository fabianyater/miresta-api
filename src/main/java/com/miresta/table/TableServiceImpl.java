package com.miresta.table;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TableServiceImpl implements ITableService {
    private final DiningTableRepository diningTableRepository;
    private final DiningTableStatusRepository diningTableStatusRepository;

    @Override
    public TableSummaryResponse getTablesInfo() {
        List<TableEntityDto> tables = diningTableRepository.findAllAsDto().stream()
                .sorted(Comparator.comparing(TableEntityDto::number))
                .toList();
        Object result = diningTableRepository.countAllStatuses();

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

        diningTableRepository.save(diningTable);
    }

    @Transactional
    @Override
    public TableEntityDto createTable(TableRequest request) {
        if (request.number() == null || request.number() <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser positivo");
        }
        if (diningTableRepository.existsByNumber(request.number())) {
            throw new EntityExistsException("Ya existe una mesa con el número " + request.number());
        }

        DiningTableStatus open = diningTableStatusRepository.findByName("OPEN");

        DiningTable table = new DiningTable();
        table.setNumber(request.number());
        table.setStatus(open);

        DiningTable saved = diningTableRepository.save(table);

        return new TableEntityDto(saved.getId(), saved.getNumber(), saved.getStatus().getName());
    }

    @Transactional
    @Override
    public TableEntityDto renameTable(Long id, TableRequest request) {
        if (request.number() == null || request.number() <= 0) {
            throw new IllegalArgumentException("El número de mesa debe ser positivo");
        }

        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada: " + id));

        diningTableRepository.findByNumber(request.number())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new EntityExistsException("Ya existe una mesa con el número " + request.number());
                });

        table.setNumber(request.number());
        DiningTable saved = diningTableRepository.save(table);

        return new TableEntityDto(saved.getId(), saved.getNumber(), saved.getStatus().getName());
    }

    @Transactional
    @Override
    public void deleteTable(Long id) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada: " + id));

        if ("IN_USE".equals(table.getStatus().getName())) {
            throw new IllegalStateException("No se puede eliminar una mesa en uso");
        }

        try {
            diningTableRepository.delete(table);
            diningTableRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar la mesa: tiene pedidos asociados");
        }
    }
}
