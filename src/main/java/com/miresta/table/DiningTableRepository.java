package com.miresta.table;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Replaces the old duplicate pair (DiningRepository / TableEntityRepository) that both
 * mapped JpaRepository&lt;DiningTable, Long&gt; and were injected into different services.
 */
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    @Query("""
        select new com.miresta.table.TableEntityDto(
            t.id, t.number, s.name, sal.id, sal.name, t.positionX, t.positionY)
        from DiningTable t
        join t.status s
        join t.salon sal
    """)
    List<TableEntityDto> findAllAsDto();

    long countBySalon_Id(Long salonId);

    List<DiningTable> findBySalon_Id(Long salonId);

    @Query("""
        select\s
            sum(case when s.name = 'OPEN' then 1 else 0 end),
            sum(case when s.name = 'IN_USE' then 1 else 0 end)
        from DiningTable t
        join t.status s
   \s""")
    Object countAllStatuses();

    boolean existsByNumber(Long number);

    Optional<DiningTable> findByNumber(Long number);

    /**
     * Same lookup as findById, but takes a row lock — used when creating an order for
     * a table so two near-simultaneous requests (two waiters, or a retry from a flaky
     * connection) can't both see "no pending order yet" and each create their own,
     * splitting the table's order into two separate tickets. The second request just
     * waits for the first to commit, then correctly finds and reuses what it created.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);
}
