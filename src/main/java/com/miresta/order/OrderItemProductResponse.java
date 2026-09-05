package com.miresta.order;

import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;

public record OrderItemProductResponse(Long id,
                                       String name,
                                       Long quantity,
                                       Money unitExtraPrice,
                                       Money catalogPrice,
                                       ComboCategory replacementCategory,
                                       /** This selection's own price — null when it's part of a combo's
                                        * flat price instead (e.g. the sopa/proteína/acompañante that make
                                        * up an "almuerzo completo"). See PricingCalculator#lineTotalFor. */
                                       Money lineTotal) {
}
