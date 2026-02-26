package com.miresta.repository;

import com.miresta.dto.response.TableEntityDto;
import com.miresta.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TableEntityRepository extends JpaRepository<DiningTable, Long> {
    @Query("""
        select new com.miresta.dto.response.TableEntityDto(t.id, t.number, s.name)
        from DiningTable t
        join t.status s
    """)
    List<TableEntityDto> findAllAsDto();

    @Query("""
        select\s
            sum(case when s.name = 'OPEN' then 1 else 0 end),
            sum(case when s.name = 'IN_USE' then 1 else 0 end)
        from DiningTable t
        join t.status s
   \s""")
    Object countAllStatuses();

}