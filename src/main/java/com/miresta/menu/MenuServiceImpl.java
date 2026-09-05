package com.miresta.menu;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miresta.catalog.ProductDto;
import com.miresta.order.OrderItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

record ProductRow(Long id, String name, Integer quantity) {
}

@RequiredArgsConstructor
@Service
@Slf4j
public class MenuServiceImpl implements IMenuService {
    private final MenuRepository menuRepository;
    private final MenuItemServiceImpl menuItemService;
    private final MenuOfferingServiceImpl menuOfferingService;
    private final OrderItemRepository orderItemRepository;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public List<MenuResponse> getMenuInfo(LocalDate date) {
        var rows = menuRepository.findMenusByDate(date);
        record Key(Long id, String type, LocalDate d) {
        }
        Map<Key, Map<String, List<ProductDto>>> acc = new LinkedHashMap<>();

        for (var r : rows) {
            var key = new Key(r.getMenuId(), r.getFoodType(), r.getMenuDate());
            acc.computeIfAbsent(key, k -> new LinkedHashMap<>());
            var catMap = acc.get(key);
            catMap.computeIfAbsent(r.getCategory(), c -> new ArrayList<>());
            var list = catMap.get(r.getCategory());

            var products = objectMapper.readValue(
                    r.getProducts(),
                    new TypeReference<List<ProductRow>>() {
                    }
            );

            for (var pr : products) {
                list.add(new ProductDto(pr.id(), pr.name(), pr.quantity()));
            }
        }

        List<MenuResponse> out = new ArrayList<>();
        for (var e : acc.entrySet()) {
            var key = e.getKey();
            var items = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(en -> new ItemResponse(
                            en.getKey(),
                            en.getValue().stream()
                                    .sorted(Comparator.comparing(ProductDto::name))
                                    .toList()))
                    .toList();

            var menu = new MenuResponse(
                    key.id(),
                    key.d().toString(),
                    key.type(),
                    items
            );
            out.add(menu);
        }

        out.sort(Comparator
                .comparing(MenuResponse::date)
                .thenComparing(MenuResponse::type));

        return out;
    }

    @Transactional()
    @Override
    public void createMenu(CreateMenuRequest request) {
        log.info("Menu request: {}", request);

        // Upserted by (date, foodType): submitting this again for a day/meal that
        // already has a menu just replaces its items — that's what makes "create the
        // menu again" work as an edit instead of piling up duplicate offerings.
        Menu menu = menuRepository.findByDate(request.date())
                .orElseGet(() -> {
                    Menu m = new Menu();
                    m.setDate(request.date());
                    return menuRepository.save(m);
                });

        MenuOffering menuOffering = menuOfferingService.getOrCreateMenuOffering(menu, request.foodType());

        menuItemService.replaceMenuItems(menuOffering, request.products());
    }

    @Transactional
    @Override
    public void deleteMenu(LocalDate date, String foodType) {
        Menu menu = menuRepository.findByDate(date)
                .orElseThrow(() -> new EntityNotFoundException("No hay menú para la fecha: " + date));

        MenuOffering menuOffering = menuOfferingService.getMenuOfferingByMenuIdAndFoodTypeName(menu.getId(), foodType);

        if (orderItemRepository.existsByMenuOffering_Id(menuOffering.getId())) {
            // Even a CANCELLED order still references this offering (order_items keeps a
            // hard FK to it for history) — deleting would violate that constraint, so this
            // is refused regardless of the order's current status, not just active ones.
            throw new IllegalStateException(
                    "No se puede eliminar: hay pedidos (incluidos cancelados) registrados con este menú. Puedes editarlo en su lugar.");
        }

        menuItemService.replaceMenuItems(menuOffering, List.of());
        menuOfferingService.deleteMenuOffering(menuOffering);

        if (menuOfferingService.getOfferingsForMenu(menu).isEmpty()) {
            menuRepository.delete(menu);
        }
    }
}
