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
    public SalonLayoutResponse getLayout(Long salonId) {
        getSalonOrThrow(salonId);
        return salonLayoutRepository.findBySalon_Id(salonId).map(this::toLayoutResponse).orElse(null);
    }

    /** Guarda (o sobreescribe) la foto de dónde está cada mesa de este salón ahora mismo. */
    @Transactional
    @Override
    public SalonLayoutResponse saveLayout(Long salonId, String actingUserEmail) {
        Salon salon = getSalonOrThrow(salonId);
        List<DiningTable> tables = diningTableRepository.findBySalon_Id(salonId);

        SalonLayout layout = salonLayoutRepository.findBySalon_Id(salonId).orElseGet(() -> {
            SalonLayout created = new SalonLayout();
            created.setSalon(salon);
            return created;
        });
        layout.setSavedAt(Instant.now());
        layout.setSavedBy(resolveName(actingUserEmail));
        layout.getPositions().clear();
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

    /** Copia las posiciones guardadas a las mesas reales — una mesa que ya no esté en
     * este salón (se movió a otro después de guardar) se deja como está. */
    @Transactional
    @Override
    public void applyLayout(Long salonId) {
        getSalonOrThrow(salonId);
        SalonLayout layout = salonLayoutRepository.findBySalon_Id(salonId)
                .orElseThrow(() -> new IllegalStateException("Este salón no tiene un plano guardado todavía."));

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

    private String resolveName(String email) {
        return userRepository.findByEmail(email)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(email);
    }

    private SalonLayoutResponse toLayoutResponse(SalonLayout layout) {
        return new SalonLayoutResponse(
                layout.getSalon().getId(), layout.getSavedAt(), layout.getSavedBy(), layout.getPositions().size());
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
