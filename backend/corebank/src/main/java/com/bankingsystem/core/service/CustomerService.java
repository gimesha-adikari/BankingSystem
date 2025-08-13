package com.bankingsystem.core.service;

import com.bankingsystem.core.dto.CustomerRequestDTO;
import com.bankingsystem.core.dto.CustomerResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    List<CustomerResponseDTO> getAllCustomers();
    CustomerResponseDTO getCustomerById(UUID id);
    CustomerResponseDTO createCustomer(CustomerRequestDTO request);
    CustomerResponseDTO updateCustomer(CustomerRequestDTO request);
    void deleteCustomer(UUID id);
}
