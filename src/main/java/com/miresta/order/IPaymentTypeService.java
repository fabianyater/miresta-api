package com.miresta.order;

import java.util.List;

public interface IPaymentTypeService {
    List<PaymentTypeResponse> getPaymentTypes();

    PaymentType getPaymentTypeById(Long id);
}
