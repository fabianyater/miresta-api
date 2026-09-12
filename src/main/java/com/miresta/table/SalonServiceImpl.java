package com.miresta.table;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SalonServiceImpl implements ISalonService {

    private final SalonRepository salonRepository;
    private final DiningTableRepository diningTableRepository;

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
