package com.miresta.order.pricing;

import com.miresta.shared.Money;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PriceSettingService {

    private final PriceSettingRepository priceSettingRepository;
    private final PriceSettingHistoryRepository priceSettingHistoryRepository;

    /**
     * Every PriceCode is looked up by fixed name throughout PricingCalculator — unlike
     * a customer or a user, there's no "unused" price row, so deleting one can't be
     * allowed to crash order pricing later. A deleted/never-configured code is treated
     * as $0 here: that component becomes free until someone re-configures it (which is
     * just editing it again — see update()'s find-or-create).
     */
    public Money amountFor(PriceCode code) {
        return priceSettingRepository.findByCode(code).map(PriceSetting::getAmount).orElse(Money.ZERO);
    }

    public List<PriceSettingResponse> getAll() {
        Map<PriceCode, PriceSetting> byCode = priceSettingRepository.findAll().stream()
                .collect(Collectors.toMap(PriceSetting::getCode, Function.identity()));

        return Arrays.stream(PriceCode.values())
                .map(code -> {
                    PriceSetting setting = byCode.get(code);
                    return setting != null ? toResponse(setting) : placeholderResponse(code);
                })
                .toList();
    }

    @Transactional
    public PriceSettingResponse update(PriceCode code, UpdatePriceSettingRequest request, String changedByEmail) {
        PriceSetting setting = priceSettingRepository.findByCode(code).orElseGet(() -> {
            PriceSetting fresh = new PriceSetting();
            fresh.setCode(code);
            fresh.setLabel(code.name());
            fresh.setAmount(Money.ZERO);
            fresh.setConfirmed(false);
            return fresh;
        });

        long previousAmount = setting.getAmount().amount();
        String previousLabel = setting.getLabel();
        boolean previousConfirmed = setting.isConfirmed();

        if (request.amount() != null) {
            setting.setAmount(Money.of(request.amount()));
        }
        if (request.label() != null && !request.label().isBlank()) {
            setting.setLabel(request.label());
        }
        if (request.confirmed() != null) {
            setting.setConfirmed(request.confirmed());
        }

        // Persist first — a recreated (previously deleted) row needs a real id before
        // a history row can reference it via the price_setting_id FK.
        PriceSetting saved = priceSettingRepository.save(setting);

        boolean changed = previousAmount != saved.getAmount().amount()
                || !previousLabel.equals(saved.getLabel())
                || previousConfirmed != saved.isConfirmed();

        if (changed) {
            PriceSettingHistory history = new PriceSettingHistory();
            history.setPriceSetting(saved);
            history.setPreviousAmount(previousAmount);
            history.setNewAmount(saved.getAmount().amount());
            history.setPreviousLabel(previousLabel);
            history.setNewLabel(saved.getLabel());
            history.setPreviousConfirmed(previousConfirmed);
            history.setNewConfirmed(saved.isConfirmed());
            history.setChangedBy(changedByEmail);
            history.setChangedAt(Instant.now());
            priceSettingHistoryRepository.save(history);
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(PriceCode code) {
        PriceSetting setting = priceSettingRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Precio no encontrado: " + code));
        // Its history rows cascade-delete with it (see V14 migration) — a fully
        // removed price starts clean if it's ever reconfigured again.
        priceSettingRepository.delete(setting);
    }

    public List<PriceSettingHistoryResponse> getHistory(PriceCode code) {
        return priceSettingHistoryRepository.findByPriceSetting_CodeOrderByChangedAtDesc(code).stream()
                .map(h -> new PriceSettingHistoryResponse(
                        h.getId(),
                        h.getPreviousAmount(),
                        h.getNewAmount(),
                        h.getPreviousLabel(),
                        h.getNewLabel(),
                        h.isPreviousConfirmed(),
                        h.isNewConfirmed(),
                        h.getChangedBy(),
                        h.getChangedAt()))
                .toList();
    }

    private PriceSettingResponse toResponse(PriceSetting setting) {
        return new PriceSettingResponse(
                setting.getCode(),
                setting.getLabel(),
                setting.getAmount(),
                setting.isConfirmed(),
                true);
    }

    private PriceSettingResponse placeholderResponse(PriceCode code) {
        return new PriceSettingResponse(code, code.name(), Money.ZERO, false, false);
    }
}
