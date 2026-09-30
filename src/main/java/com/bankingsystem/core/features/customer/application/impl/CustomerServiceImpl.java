package com.bankingsystem.core.features.customer.application.impl;

import com.bankingsystem.core.features.customer.interfaces.dto.CustomerRequestDTO;
import com.bankingsystem.core.features.customer.interfaces.dto.CustomerResponseDTO;
import com.bankingsystem.core.features.customer.domain.Customer;
import com.bankingsystem.core.features.auth.domain.User;
import com.bankingsystem.core.modules.common.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.features.customer.domain.repository.CustomerRepository;
import com.bankingsystem.core.features.auth.domain.repository.UserRepository;
import com.bankingsystem.core.features.customer.application.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return mapToDTO(customer);
    }

    @Override
    public CustomerResponseDTO createCustomer(CustomerRequestDTO request) {
        Customer customer = mapToEntity(request);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
        return mapToDTO(customer);
    }

    @Override
    public CustomerResponseDTO updateCustomer(CustomerRequestDTO request) {
        Customer customer = customerRepository.findByUserUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setGender(request.getGender());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(request.getStatus());
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
        return mapToDTO(customer);
    }

    @Override
    public void deleteCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customerRepository.delete(customer);
    }

    @Override
    public CustomerResponseDTO upsertByUser(CustomerRequestDTO request) {
        return customerRepository.findByUserUserId(request.getUserId())
                .map(existing -> {
                    existing.setFirstName(request.getFirstName());
                    existing.setLastName(request.getLastName());
                    existing.setGender(request.getGender());
                    existing.setEmail(request.getEmail());
                    existing.setPhone(request.getPhone());
                    existing.setAddress(request.getAddress());
                    existing.setDateOfBirth(request.getDateOfBirth());
                    if (request.getStatus() != null) {
                        existing.setStatus(request.getStatus());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    customerRepository.save(existing);
                    return mapToDTO(existing);
                })
                .orElseGet(() -> createCustomer(request));
    }

    // NEW: fetch by owning user id
    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getByUserId(UUID userId) {
        Customer customer = customerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return mapToDTO(customer);
    }

    private Customer mapToEntity(CustomerRequestDTO dto) {
        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setGender(dto.getGender());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setStatus(dto.getStatus());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User found for customer {}", user.getUsername());
        customer.setUser(user);
        customer.getUser().setCustomer(customer);
        return customer;
    }

    private CustomerResponseDTO mapToDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setCustomerId(customer.getCustomerId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setGender(customer.getGender());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setDateOfBirth(String.valueOf(customer.getDateOfBirth()));
        dto.setStatus(customer.getStatus());
        dto.setCreatedAt(customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null);
        dto.setUpdatedAt(customer.getUpdatedAt() != null ? customer.getUpdatedAt().toString() : null);
        if (customer.getUser() != null) {
            dto.setUsername(customer.getUser().getUsername());
            dto.setUserId(customer.getUser().getUserId());
        }
        return dto;
    }
}
