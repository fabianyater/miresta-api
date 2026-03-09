package com.miresta.services.impl;

import com.miresta.dto.response.ProductWithIdAndQuantity;
import com.miresta.entity.MenuItem;
import com.miresta.entity.MenuService;
import com.miresta.entity.Product;
import com.miresta.repository.MenuItemRepository;
import com.miresta.services.IMenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MenuItemServiceImpl implements IMenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final ProductServiceImpl productService;
    private final MenuServicesServiceImpl menuServicesService;

    @Override
    public void createMenuItem(MenuService menuService, ProductWithIdAndQuantity productWIthIdAndQuantity) {
        Product product = productService.getProductById(productWIthIdAndQuantity.id());

        MenuItem menuItem = new MenuItem();
        menuItem.setMenuService(menuService);
        menuItem.setProduct(product);
        menuItem.setQuantity(productWIthIdAndQuantity.quantity().intValue());

        menuItemRepository.save(menuItem);
    }
}
