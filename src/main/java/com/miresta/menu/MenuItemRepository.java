package com.miresta.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByMenuOffering_Id(Long menuOfferingId);

    Optional<MenuItem> findByMenuOffering_IdAndProduct_Id(Long menuOfferingId, Long productId);

    void deleteByMenuOffering_Id(Long menuOfferingId);

    /** Only matches (and only decrements) when there's actually enough left — the
     * caller checks the affected-row count to know whether it succeeded. */
    @Modifying
    @Query("update MenuItem m set m.quantity = m.quantity - :amount where m.id = :id and m.quantity >= :amount")
    int decrementQuantity(@Param("id") Long id, @Param("amount") long amount);

    @Modifying
    @Query("update MenuItem m set m.quantity = m.quantity + :amount where m.id = :id and m.quantity is not null")
    int restoreQuantity(@Param("id") Long id, @Param("amount") long amount);
}
