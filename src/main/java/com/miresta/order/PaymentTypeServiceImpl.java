package com.miresta.order;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentTypeServiceImpl implements IPaymentTypeService {
    private final PaymentTypeRepository paymentTypeRepository;

    @Override
    public List<PaymentTypeResponse> getPaymentTypes() {
        return paymentTypeRepository.findAll().stream()
                .map(pt -> new PaymentTypeResponse(pt.getId(), pt.getName()))
                .toList();
    }

    @Override
    public PaymentType getPaymentTypeById(Long id) {
        return paymentTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment type not found: " + id));
    }
}
