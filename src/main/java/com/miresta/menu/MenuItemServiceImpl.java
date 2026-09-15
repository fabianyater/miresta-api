package com.miresta.menu;

import com.miresta.catalog.Product;
import com.miresta.catalog.ProductServiceImpl;
import com.miresta.catalog.ProductWithIdAndQuantity;
import com.miresta.shared.StockEventBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MenuItemServiceImpl implements IMenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final ProductServiceImpl productService;
    private final StockEventBroadcaster stockEventBroadcaster;

    @Override
    public void createMenuItem(MenuOffering menuOffering, ProductWithIdAndQuantity productWithIdAndQuantity) {
        Product product = productService.getProductById(productWithIdAndQuantity.id());
        Integer quantity = productWithIdAndQuantity.quantity() != null ? productWithIdAndQuantity.quantity().intValue() : null;

        MenuItem menuItem = new MenuItem();
        menuItem.setMenuOffering(menuOffering);
        menuItem.setProduct(product);
        menuItem.setQuantity(quantity);
        menuItem.setInitialQuantity(quantity);

        menuItemRepository.save(menuItem);
    }

    @Transactional
    @Override
    public void replaceMenuItems(MenuOffering menuOffering, List<ProductWithIdAndQuantity> products) {
        Map<Long, MenuItem> existingByProduct = menuItemRepository.findByMenuOffering_Id(menuOffering.getId()).stream()
                .collect(Collectors.toMap(item -> item.getProduct().getId(), item -> item));

        for (ProductWithIdAndQuantity p : products) {
            MenuItem existing = existingByProduct.remove(p.id());
            Integer newCap = p.quantity() != null ? p.quantity().intValue() : null;

            if (existing == null) {
                createMenuItem(menuOffering, p);
                continue;
            }

            if (newCap == null || existing.getInitialQuantity() == null) {
                // No cap before, none now (or the cap is being lifted entirely) — nothing
                // to preserve, there was no consumption being tracked either way.
                existing.setInitialQuantity(newCap);
                existing.setQuantity(newCap);
            } else {
                // Re-deriving "what's left" from the NEW cap minus what's actually been
                // consumed so far — not just overwriting quantity with the submitted
                // number — is what keeps a menu edit from wiping out today's sales.
                long consumedSoFar = existing.getInitialQuantity() - existing.getQuantity();
                existing.setInitialQuantity(newCap);
                existing.setQuantity((int) Math.max(0L, newCap - consumedSoFar));
            }
            menuItemRepository.save(existing);
        }

        // Whatever's left in existingByProduct is no longer on the menu — the admin
        // took it off, so it's fine to drop (nothing to preserve for a product that's
        // not being offered anymore).
        existingByProduct.values().forEach(menuItemRepository::delete);

        stockEventBroadcaster.notifyStockChanged();
    }

    @Transactional
    @Override
    public void consume(MenuOffering menuOffering, Product product, long amount) {
        menuItemRepository.findByMenuOffering_IdAndProduct_Id(menuOffering.getId(), product.getId())
                .filter(item -> item.getQuantity() != null)
                .ifPresent(item -> {
                    int updated = menuItemRepository.decrementQuantity(item.getId(), amount);
                    if (updated == 0) {
                        throw new IllegalStateException(
                                "No queda suficiente " + product.getName() + " disponible hoy.");
                    }
                    stockEventBroadcaster.notifyStockChanged();
                });
    }

    @Transactional
    @Override
    public void restore(MenuOffering menuOffering, Product product, long amount) {
        menuItemRepository.findByMenuOffering_IdAndProduct_Id(menuOffering.getId(), product.getId())
                .ifPresent(item -> {
                    menuItemRepository.restoreQuantity(item.getId(), amount);
                    stockEventBroadcaster.notifyStockChanged();
                });
    }
}
