package com.miresta.kitchen;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitchenPhraseService {

    private static final int MAX_PHRASES = 24;
    private static final int MAX_LENGTH = 60;

    private final KitchenPhraseRepository repository;

    @Transactional(readOnly = true)
    public List<String> list() {
        return repository.findAllByOrderBySortOrderAsc().stream()
                .map(KitchenPhrase::getText)
                .toList();
    }

    /** Reemplaza toda la lista con la que llegue, ya limpia (sin vacíos ni repetidas). */
    @Transactional
    public List<String> replace(List<String> incoming) {
        List<String> cleaned = new ArrayList<>(new LinkedHashSet<>(
                (incoming == null ? List.<String>of() : incoming).stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().length() > MAX_LENGTH ? s.trim().substring(0, MAX_LENGTH) : s.trim())
                        .toList()));

        if (cleaned.size() > MAX_PHRASES) {
            cleaned = cleaned.subList(0, MAX_PHRASES);
        }

        repository.deleteAllInBatch();
        int order = 0;
        for (String text : cleaned) {
            KitchenPhrase phrase = new KitchenPhrase();
            phrase.setText(text);
            phrase.setSortOrder(order++);
            repository.save(phrase);
        }
        return cleaned;
    }
}
