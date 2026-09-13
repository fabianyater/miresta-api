package com.miresta.table;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TableServiceImpl implements ITableService {

    // Cuadrícula del plano de un salón — 8x5 casillas (calzan con el lienzo 16:10 del
    // frontend en casillas cuadradas). Toda mesa vive en el centro de una casilla; el
    // frontend es quien impide soltarla fuera de la cuadrícula o encima de otra mesa.
    private static final int GRID_COLUMNS = 8;
    private static final int GRID_ROWS = 5;

    private final DiningTableRepository diningTableRepository;
    private final DiningTableStatusRepository diningTableStatusRepository;
    private final SalonRepository salonRepository;

    @Override
    public TableSummaryResponse getTablesInfo() {
        List<TableEntityDto> raw = diningTableRepository.findAllAsDto();
        Map<Long, TableEntityDto> byId = raw.stream()
                .collect(Collectors.toMap(TableEntityDto::id, dto -> dto));

        // Una mesa unida a otra no tiene pedido propio — su estado real (libre/en
        // uso) es el de la principal, aunque en la base siga marcada libre.
        List<TableEntityDto> tables = raw.stream()
                .map(dto -> {
                    if (dto.mergedIntoId() == null) return dto;
                    TableEntityDto primary = byId.get(dto.mergedIntoId());
                    if (primary == null || primary.status().equals(dto.status())) return dto;
                    return new TableEntityDto(
                            dto.id(), dto.number(), primary.status(), dto.salonId(), dto.salonName(),
                            dto.positionX(), dto.positionY(), dto.mergedIntoId());
                })
                .sorted(Comparator.comparing(TableEntityDto::number))
                .toList();

        long free = tables.stream().filter(t -> "OPEN".equals(t.status())).count();
        long inUse = tables.stream().filter(t -> "IN_USE".equals(t.status())).count();

        return new TableSummaryResponse(tables, free, inUse);
    }

    @Transactional
    @Override
    public void updateTableStatus(DiningTable diningTable, String status) {
        DiningTableStatus diningTableStatus = diningTableStatusRepository.findByName(status);

        diningTable.setStatus(diningTableStatus);

        diningTableRepository.save(diningTable);

        // Al liberar una mesa que era ancla de un grupo unido, el grupo se disuelve
        // solo — ya no hay una cuenta compartida corriendo.
        if ("OPEN".equals(status)) {
            unmergeTables(diningTable.getId());
        }
    }

    @Transactional
    @Override
    public void mergeTables(Long primaryId, List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos una mesa para unir.");
        }
        DiningTable primary = getTableOrThrow(primaryId);
        if (primary.getMergedInto() != null) {
            throw new IllegalStateException("Esa mesa ya está unida a otra — sepárala primero.");
        }
        if (!"OPEN".equals(primary.getStatus().getName())) {
            throw new IllegalStateException("Solo se pueden unir mesas libres.");
        }

        for (Long id : tableIds) {
            if (id.equals(primaryId)) continue;
            DiningTable table = getTableOrThrow(id);
            if (!"OPEN".equals(table.getStatus().getName())) {
                throw new IllegalStateException("La mesa " + table.getNumber() + " no está libre.");
            }
            if (table.getMergedInto() != null) {
                throw new IllegalStateException("La mesa " + table.getNumber() + " ya está unida a otra.");
            }
            if (diningTableRepository.existsByMergedInto_Id(id)) {
                throw new IllegalStateException("La mesa " + table.getNumber() + " ya es principal de otro grupo.");
            }
            table.setMergedInto(primary);
            diningTableRepository.save(table);
        }
    }

    @Transactional
    @Override
    public void unmergeTables(Long primaryId) {
        for (DiningTable member : diningTableRepository.findByMergedInto_Id(primaryId)) {
            member.setMergedInto(null);
            diningTableRepository.save(member);
        }
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
            Salon newSalon = getSalonOrThrow(request.salonId());
            table.setSalon(newSalon);
            // Nueva sección, nueva casilla — la posición vieja era relativa al plano del
            // salón anterior y podría chocar con una mesa que ya esté ahí.
            float[] position = nextGridPosition(newSalon.getId());
            table.setPositionX(position[0]);
            table.setPositionY(position[1]);
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
        if (table.getMergedInto() != null || diningTableRepository.existsByMergedInto_Id(id)) {
            throw new IllegalStateException("No se puede eliminar una mesa unida a otra — sepárala primero.");
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

    private DiningTable getTableOrThrow(Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada: " + id));
    }

    private float[] nextGridPosition(Long salonId) {
        int index = (int) diningTableRepository.countBySalon_Id(salonId);
        int col = index % GRID_COLUMNS;
        int row = (index / GRID_COLUMNS) % GRID_ROWS;
        float x = (col + 0.5f) / GRID_COLUMNS * 100f;
        float y = (row + 0.5f) / GRID_ROWS * 100f;
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
                table.getPositionY(),
                table.getMergedInto() != null ? table.getMergedInto().getId() : null);
    }
}
