package com.miresta.kitchen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitchenPhraseRepository extends JpaRepository<KitchenPhrase, Long> {
    List<KitchenPhrase> findAllByOrderBySortOrderAsc();
}
