package com.miresta.catalog;

import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getCategories();

    Category getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}
