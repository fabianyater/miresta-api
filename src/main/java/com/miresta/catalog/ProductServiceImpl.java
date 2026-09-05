package com.miresta.catalog;

import com.miresta.shared.Money;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductServiceImpl implements IProductService {
    private final ProductRepository productRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final CategoryServiceImpl categoryServiceImpl;
    private final ProductDetailsServiceImpl productDetailsServiceImpl;

    @Transactional
    @Override
    public void createProduct(ProductRequest productRequest) {
        Category category = categoryServiceImpl.getCategoryById(productRequest.categoryId());
        Product product = new Product();

        product.setName(productRequest.name());
        product.setCategory(category);

        var savedProduct = productRepository.save(product);

        if (productRequest.expirationDate() != null && productRequest.quantity() != null && productRequest.unitPrice() > 0) {
            ProductDetails productDetails = new ProductDetails();

            productDetails.setProduct(savedProduct);
            productDetails.setQuantity(productRequest.quantity());
            productDetails.setExpirationDate(productRequest.expirationDate());
            productDetails.setPrice(Money.of(productRequest.unitPrice()));

            productDetailsServiceImpl.createProductDetails(productDetails);
        }
    }

    @Transactional
    @Override
    public ProductDetailResponse getProductDetail(Long productId) {
        return toDetailResponse(getProductById(productId));
    }

    @Transactional
    @Override
    public ProductDetailResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = getProductById(productId);

        product.setName(request.name());
        product.setCategory(categoryServiceImpl.getCategoryById(request.categoryId()));
        product.setActsAsCategory(request.actsAsCategory());

        Product saved = productRepository.save(product);

        ProductDetails existing = saved.getProductDetails().stream().findFirst().orElse(null);
        boolean hasDetail = request.expirationDate() != null || request.quantity() != null || request.unitPrice() != null;

        if (hasDetail) {
            ProductDetails details = existing != null ? existing : new ProductDetails();
            details.setProduct(saved);
            details.setExpirationDate(request.expirationDate());
            details.setQuantity(request.quantity());
            details.setPrice(Money.of(request.unitPrice() != null ? request.unitPrice() : 0L));
            ProductDetails savedDetails = productDetailsRepository.save(details);
            if (existing == null) {
                saved.getProductDetails().add(savedDetails);
            }
        } else if (existing != null) {
            // orphanRemoval on Product.productDetails deletes it on flush.
            saved.getProductDetails().remove(existing);
        }

        return toDetailResponse(saved);
    }

    @Transactional
    @Override
    public void deleteProduct(Long productId) {
        Product product = getProductById(productId);
        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar un producto que ya se usó en un menú o un pedido. Puedes editarlo en su lugar.");
        }
    }

    @Override
    public List<ProductInfo> getProducts() {
        return productRepository.findAllProductsWithCategory();
    }

    @Override
    public List<ProductWithDetails> getProductsWithDetails() {
        return productRepository.findAllProductsWithDetails().stream()
                .sorted(Comparator.comparing(o -> o.getCategory().getName()))
                .toList();
    }

    @Override
    public List<ProductWithDetails> getProductsByCategory() {
        return productRepository.findAllByCategoryName("Bebidas");
    }

    @Override
    public List<ProductInfo> getAdditionalCommonProducts() {
        String[] commonProducts = {"Huevo cocido", "Huevo frito", "Huevos revueltos"};
        return productRepository.findAllByNameIn(List.of(commonProducts), "Adicionales");
    }

    @Override
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + productId));
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        ProductDetails d = product.getProductDetails().stream().findFirst().orElse(null);
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getActsAsCategory(),
                d != null ? d.getExpirationDate() : null,
                d != null ? d.getQuantity() : null,
                d != null && d.getPrice() != null ? d.getPrice().amount() : null);
    }
}
