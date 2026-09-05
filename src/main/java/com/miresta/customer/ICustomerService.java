package com.miresta.customer;

import java.util.List;

public interface ICustomerService {
    CustomerResponse createCustomer(CustomerRequest request);

    Customer getCustomerById(Long id);

    CustomerResponse getCustomerResponseById(Long id);

    CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request);

    void deleteCustomer(Long id);

    List<CustomerResponse> searchByPhone(String phone);

    List<CustomerResponse> getCustomers();
}
