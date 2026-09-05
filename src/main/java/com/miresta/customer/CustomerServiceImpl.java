package com.miresta.customer;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements ICustomerService {
    private final CustomerRepository customerRepository;

    @Transactional
    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        customerRepository.findByPhone(request.phone()).ifPresent(existing -> {
            throw new EntityExistsException(
                    "Ya existe un cliente registrado con ese teléfono: " + existing.getName());
        });

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setCreatedAt(Instant.now());

        return toResponse(customerRepository.save(customer));
    }

    @Override
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    @Override
    public CustomerResponse getCustomerResponseById(Long id) {
        return toResponse(getCustomerById(id));
    }

    @Transactional
    @Override
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = getCustomerById(id);

        if (request.phone() != null && !request.phone().equals(customer.getPhone())) {
            customerRepository.findByPhone(request.phone()).ifPresent(existing -> {
                throw new EntityExistsException(
                        "Ya existe un cliente registrado con ese teléfono: " + existing.getName());
            });
            customer.setPhone(request.phone());
        }
        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.active() != null) {
            customer.setActive(request.active());
        }

        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    @Override
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        try {
            customerRepository.delete(customer);
            customerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "No se puede eliminar un cliente con pedidos registrados. Archívalo en su lugar.");
        }
    }

    @Override
    public List<CustomerResponse> searchByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .map(this::toResponse)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    public List<CustomerResponse> getCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(), customer.isActive());
    }
}
