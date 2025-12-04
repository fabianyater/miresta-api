package com.miresta.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miresta.dto.request.CreateMenuRequest;
import com.miresta.dto.response.ItemResponse;
import com.miresta.dto.response.MenuResponse;
import com.miresta.dto.response.ProductDto;
import com.miresta.entity.Menu;
import com.miresta.entity.MenuService;
import com.miresta.repository.MenuRepository;
import com.miresta.services.IMenuService;
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
    private final FoodTypeServiceImpl foodTypeService;
    private final ProductServiceImpl productService;
    private final MenuItemServiceImpl menuItemService;
    private final MenuServicesServiceImpl menuServicesService;

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
        Menu menu = new Menu();

        if (request.menuId() == null || request.menuId().isBlank()) {
            menu.setDate(request.date());

            Menu savedMenu = menuRepository.save(menu);
            MenuService savedMenuService = menuServicesService.createMenuServices(savedMenu, request.foodType());

            request.products().forEach(productWIthIdAndQuantity -> {
                menuItemService.createMenuItem(savedMenuService, productWIthIdAndQuantity);
            });

            return;
        }

        Menu existingMenu = menuRepository.findById(Long.valueOf(request.menuId()))
                .orElseThrow(() -> new RuntimeException("Menu not found: " + request.menuId()));

        MenuService savedMenuService = menuServicesService.createMenuServices(existingMenu, request.foodType());

        request.products().forEach(productWIthIdAndQuantity -> {
            menuItemService.createMenuItem(savedMenuService, productWIthIdAndQuantity);
        });

    }
}
