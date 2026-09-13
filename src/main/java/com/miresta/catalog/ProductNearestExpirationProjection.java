package com.miresta.catalog;

import java.time.LocalDate;

/** Projection for the bulk "próxima fecha de vencimiento por producto" query. */
public interface ProductNearestExpirationProjection {
    Long getProductId();

    LocalDate getExpirationDate();
}
