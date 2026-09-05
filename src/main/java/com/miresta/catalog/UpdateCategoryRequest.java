package com.miresta.catalog;

import com.miresta.shared.ComboCategory;

/** Both fields optional — only the ones present are applied. */
public record UpdateCategoryRequest(String name, ComboCategory code) {
}
