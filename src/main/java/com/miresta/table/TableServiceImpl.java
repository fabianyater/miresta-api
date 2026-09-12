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

    // Ubicación por defecto de una mesa nueva — una cuadrícula simple de 4 columnas
    // para que no quede apilada sobre otra; el admin la reacomoda en Admin -> Salones.
    private static final int GRID_COLUMNS = 4;
    private static final float GRID_SPACING = 20f;
    private static final float GRID_OFFSET = 10f;

    private final DiningTableRepository diningTableRepository;
    private final DiningTableStatusRepository diningTableStatusRepository;
    private final SalonRepository salonRepository;

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
        if (request.salonId() == null) {
            throw new IllegalArgumentException("Indica a qué salón pertenece la mesa.");
        }
        Salon salon = getSalonOrThrow(request.salonId());

        DiningTableStatus open = diningTableStatusRepository.findByName("OPEN");

        DiningTable table = new DiningTable();
        table.setNumber(request.number());
        table.setStatus(open);
        table.setSalon(salon);

        float[] position = nextGridPosition(salon.getId());
        table.setPositionX(position[0]);
        table.setPositionY(position[1]);

        DiningTable saved = diningTableRepository.save(table);

        return toDto(saved);
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
        if (request.salonId() != null && !request.salonId().equals(table.getSalon().getId())) {
            table.setSalon(getSalonOrThrow(request.salonId()));
        }
        DiningTable saved = diningTableRepository.save(table);

        return toDto(saved);
    }

    @Transactional
    @Override
    public TableEntityDto updateTablePosition(Long id, TablePositionRequest request) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada: " + id));

        table.setPositionX(clamp(request.positionX()));
        table.setPositionY(clamp(request.positionY()));

        return toDto(diningTableRepository.save(table));
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

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new EntityNotFoundException("Salón no encontrado: " + salonId));
    }

    private float[] nextGridPosition(Long salonId) {
        int index = (int) diningTableRepository.countBySalon_Id(salonId);
        float x = GRID_OFFSET + GRID_SPACING * (index % GRID_COLUMNS);
        float y = GRID_OFFSET + GRID_SPACING * (index / GRID_COLUMNS);
        return new float[]{x, y};
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    private TableEntityDto toDto(DiningTable table) {
        return new TableEntityDto(
                table.getId(),
                table.getNumber(),
                table.getStatus().getName(),
                table.getSalon().getId(),
                table.getSalon().getName(),
                table.getPositionX(),
                table.getPositionY());
    }
}
