package com.miresta.kitchen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface KitchenMessageRepository extends JpaRepository<KitchenMessage, Long> {
    List<KitchenMessage> findByCreatedAtGreaterThanOrderByCreatedAtAsc(Instant since);

    List<KitchenMessage> findTop20ByOrderByCreatedAtDesc();
}
