package com.bankingsystem.core.features.customer.application;

import com.bankingsystem.core.features.customer.interfaces.dto.CustomerRequestDTO;
import com.bankingsystem.core.features.customer.interfaces.dto.CustomerResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    List<CustomerResponseDTO> getAllCustomers();
    CustomerResponseDTO getCustomerById(UUID id);
    CustomerResponseDTO createCustomer(CustomerRequestDTO request);
    CustomerResponseDTO updateCustomer(CustomerRequestDTO request);
    void deleteCustomer(UUID id);
    CustomerResponseDTO upsertByUser(CustomerRequestDTO request);
    CustomerResponseDTO getByUserId(UUID userId);
}
