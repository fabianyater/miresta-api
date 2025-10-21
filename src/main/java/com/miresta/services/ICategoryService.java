package com.miresta.services;

import com.miresta.dto.CategoryResponse;
import com.miresta.entity.Category;

import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getCategories();
    Category getCategoryById(Long id);
}
