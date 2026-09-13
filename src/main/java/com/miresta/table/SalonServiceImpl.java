package com.miresta.table;

import com.miresta.auth.User;
import com.miresta.auth.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SalonServiceImpl implements ISalonService {

    private final SalonRepository salonRepository;
    private final DiningTableRepository diningTableRepository;
    private final SalonLayoutRepository salonLayoutRepository;
    private final UserRepository userRepository;

    @Override
    public List<SalonResponse> getSalons() {
        return salonRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public SalonResponse createSalon(SalonRequest request) {
        String name = normalizeName(request.name());
        if (salonRepository.existsByNameIgnoreCase(name)) {
            throw new EntityExistsException("Ya existe un salón llamado \"" + name + "\".");
        }

        int nextOrder = salonRepository.findTopByOrderBySortOrderDesc()
                .map(s -> s.getSortOrder() + 1)
                .orElse(0);

        Salon salon = new Salon();
        salon.setName(name);
        salon.setSortOrder(nextOrder);

        return toResponse(salonRepository.save(salon));
    }

    @Transactional
    @Override
    public SalonResponse renameSalon(Long id, SalonRequest request) {
        Salon salon = getSalonOrThrow(id);
        String name = normalizeName(request.name());

        if (salonRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new EntityExistsException("Ya existe un salón llamado \"" + name + "\".");
        }

        salon.setName(name);
        return toResponse(salonRepository.save(salon));
    }

    @Transactional
    @Override
    public SalonResponse moveSalon(Long id, String direction) {
        Salon salon = getSalonOrThrow(id);
        List<Salon> ordered = salonRepository.findAllByOrderBySortOrderAsc();
        int index = ordered.indexOf(salon);
        int targetIndex = "UP".equalsIgnoreCase(direction) ? index - 1 : index + 1;

        if (targetIndex < 0 || targetIndex >= ordered.size()) {
            return toResponse(salon);
        }

        Salon neighbor = ordered.get(targetIndex);
        int temp = salon.getSortOrder();
        salon.setSortOrder(neighbor.getSortOrder());
        neighbor.setSortOrder(temp);
        salonRepository.save(neighbor);

        return toResponse(salonRepository.save(salon));
    }

    @Transactional
    @Override
    public void deleteSalon(Long id) {
        Salon salon = getSalonOrThrow(id);
        if (diningTableRepository.countBySalon_Id(id) > 0) {
            throw new IllegalStateException("Este salón tiene mesas — muévelas a otro salón o elimínalas primero.");
        }
        salonRepository.delete(salon);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SalonLayoutResponse> getLayouts(Long salonId) {
        getSalonOrThrow(salonId);
        return salonLayoutRepository.findBySalon_IdOrderBySavedAtDesc(salonId).stream()
                .map(this::toLayoutResponse)
                .toList();
    }

    /** Guarda la foto de dónde está cada mesa de este salón ahora mismo, bajo un nombre propio. */
    @Transactional
    @Override
    public SalonLayoutResponse saveLayout(Long salonId, SalonLayoutRequest request, String actingUserEmail) {
        Salon salon = getSalonOrThrow(salonId);
        String name = normalizeLayoutName(request.name());
        if (salonLayoutRepository.existsBySalon_IdAndNameIgnoreCase(salonId, name)) {
            throw new EntityExistsException("Ya existe un plano llamado \"" + name + "\" en este salón.");
        }
        List<DiningTable> tables = diningTableRepository.findBySalon_Id(salonId);

        SalonLayout layout = new SalonLayout();
        layout.setSalon(salon);
        layout.setName(name);
        layout.setSavedAt(Instant.now());
        layout.setSavedBy(resolveName(actingUserEmail));
        for (DiningTable table : tables) {
            SalonLayoutPosition position = new SalonLayoutPosition();
            position.setLayout(layout);
            position.setTable(table);
            position.setPositionX(table.getPositionX());
            position.setPositionY(table.getPositionY());
            layout.getPositions().add(position);
        }

        return toLayoutResponse(salonLayoutRepository.save(layout));
    }

    @Transactional
    @Override
    public SalonLayoutResponse renameLayout(Long salonId, Long layoutId, SalonLayoutRequest request) {
        getSalonOrThrow(salonId);
        SalonLayout layout = getLayoutOrThrow(salonId, layoutId);
        String name = normalizeLayoutName(request.name());
        if (salonLayoutRepository.existsBySalon_IdAndNameIgnoreCaseAndIdNot(salonId, name, layoutId)) {
            throw new EntityExistsException("Ya existe un plano llamado \"" + name + "\" en este salón.");
        }
        layout.setName(name);
        return toLayoutResponse(salonLayoutRepository.save(layout));
    }

    @Transactional
    @Override
    public void deleteLayout(Long salonId, Long layoutId) {
        getSalonOrThrow(salonId);
        SalonLayout layout = getLayoutOrThrow(salonId, layoutId);
        salonLayoutRepository.delete(layout);
    }

    /** Copia las posiciones guardadas a las mesas reales — una mesa que ya no esté en
     * este salón (se movió a otro después de guardar) se deja como está. */
    @Transactional
    @Override
    public void applyLayout(Long salonId, Long layoutId) {
        getSalonOrThrow(salonId);
        SalonLayout layout = getLayoutOrThrow(salonId, layoutId);

        for (SalonLayoutPosition position : layout.getPositions()) {
            DiningTable table = position.getTable();
            if (!table.getSalon().getId().equals(salonId)) {
                continue;
            }
            table.setPositionX(position.getPositionX());
            table.setPositionY(position.getPositionY());
            diningTableRepository.save(table);
        }
    }

    private SalonLayout getLayoutOrThrow(Long salonId, Long layoutId) {
        SalonLayout layout = salonLayoutRepository.findById(layoutId)
                .orElseThrow(() -> new EntityNotFoundException("Plano no encontrado: " + layoutId));
        if (!layout.getSalon().getId().equals(salonId)) {
            throw new EntityNotFoundException("Plano no encontrado: " + layoutId);
        }
        return layout;
    }

    private String normalizeLayoutName(String name) {
        String trimmed = name != null ? name.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("El nombre del plano no puede estar vacío.");
        }
        return trimmed;
    }

    private String resolveName(String email) {
        return userRepository.findByEmail(email)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(email);
    }

    private SalonLayoutResponse toLayoutResponse(SalonLayout layout) {
        return new SalonLayoutResponse(
                layout.getId(), layout.getSalon().getId(), layout.getName(), layout.getSavedAt(),
                layout.getSavedBy(), layout.getPositions().size());
    }

    private Salon getSalonOrThrow(Long id) {
        return salonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salón no encontrado: " + id));
    }

    private String normalizeName(String name) {
        String trimmed = name != null ? name.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("El nombre del salón no puede estar vacío.");
        }
        return trimmed;
    }

    private SalonResponse toResponse(Salon salon) {
        return new SalonResponse(
                salon.getId(), salon.getName(), salon.getSortOrder(), diningTableRepository.countBySalon_Id(salon.getId()));
    }
}
