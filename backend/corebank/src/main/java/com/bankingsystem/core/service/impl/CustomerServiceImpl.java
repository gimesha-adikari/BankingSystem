package com.bankingsystem.core.service.impl;

import com.bankingsystem.core.dto.CustomerRequestDTO;
import com.bankingsystem.core.dto.CustomerResponseDTO;
import com.bankingsystem.core.entity.Customer;
import com.bankingsystem.core.entity.User;
import com.bankingsystem.core.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.repository.CustomerRepository;
import com.bankingsystem.core.repository.UserRepository;
import com.bankingsystem.core.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
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
        customer.setGender((request.getGender()));
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus((request.getStatus()));
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

    public CustomerResponseDTO mapToDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();

        dto.setCustomerId(customer.getCustomerId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setGender(customer.getGender());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());

        dto.setDateOfBirth(customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null);

        dto.setStatus(customer.getStatus());

        dto.setCreatedAt(customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null);
        dto.setUpdatedAt(customer.getUpdatedAt() != null ? customer.getUpdatedAt().toString() : null);

        if (customer.getUser() != null) {
            dto.setUserId(customer.getUser().getUserId());
            dto.setUsername(customer.getUser().getUsername());
        }

        return dto;
    }


    private Customer mapToEntity(CustomerRequestDTO dto) {
        Customer customer = new Customer();
        customer.setFirstName(dto.getFirstName());
        customer.setLastName(dto.getLastName());
        customer.setGender((dto.getGender()));
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setStatus((dto.getStatus()));
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        User user = userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User found for customer {}",user.getUsername());
        customer.setUser(user);
        customer.getUser().setCustomer(customer);
        return customer;
    }
}
