package com.miresta.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
class CustomerController {
    private final ICustomerService customerService;

    @PostMapping
    @PreAuthorize("@access.has('CLIENTES_VER')")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping
    @PreAuthorize("@access.has('CLIENTES_VER')")
    public ResponseEntity<List<CustomerResponse>> getCustomers(
            @RequestParam(value = "phone", required = false) String phone) {
        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(customerService.searchByPhone(phone));
        }
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.has('CLIENTES_VER')")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerResponseById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.has('CLIENTES_EDITAR')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id, @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.has('CLIENTES_EDITAR')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
