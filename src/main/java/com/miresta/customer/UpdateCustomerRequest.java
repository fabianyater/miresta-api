package com.miresta.customer;

public record UpdateCustomerRequest(String name, String phone, Boolean active) {
}
