package com.miresta.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    Optional<Menu> findByDate(LocalDate date);

    @Query(value = """
              SELECT
                m.id AS menu_id,
                m.date AS menu_date,
                ft.name AS food_type,
                c.name AS category,
                json_agg(json_build_object('id', p.id, 'name', p.name, 'quantity', mi.quantity) ORDER BY p.id, p.name) AS products

              FROM menu m
              JOIN menu_offering ms ON ms.menu_id = m.id
              JOIN food_type ft    ON ft.id = ms.food_type_id
              JOIN menu_item mi    ON mi.menu_offering_id = ms.id
              JOIN products p      ON p.id = mi.product_id
              JOIN categories c    ON c.id = p.category_id
              WHERE (CAST(:date AS DATE) IS NULL OR m.date = CAST(:date AS DATE))
              GROUP BY m.id, m.date, ft.name, c.name
              ORDER BY m.date, ft.name, c.name
            """, nativeQuery = true)
    List<MenuInfo> findMenusByDate(@Param("date") LocalDate date);
}
