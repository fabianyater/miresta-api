package com.miresta.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-types")
@RequiredArgsConstructor
class PaymentTypeController {
    private final IPaymentTypeService paymentTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<PaymentTypeResponse>> getPaymentTypes() {
        return ResponseEntity.ok(paymentTypeService.getPaymentTypes());
    }
}
