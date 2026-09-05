package com.miresta.order.pricing;

import com.miresta.shared.Money;

/** configured=false means this code has no row right now (deleted or never set) — it's
 *  priced as $0 in the meantime; see PriceSettingService.amountFor. */
public record PriceSettingResponse(PriceCode code, String label, Money amount, boolean confirmed, boolean configured) {
}
