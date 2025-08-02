package com.bankingsystem.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String homeNumber;
    private String workNumber;
    private String officeNumber;
    private String mobileNumber;

    public String getCity() {
        return city;
    }

    public UpdateProfileRequest setCity(String city) {
        this.city = city;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UpdateProfileRequest setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UpdateProfileRequest setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public UpdateProfileRequest setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getState() {
        return state;
    }

    public UpdateProfileRequest setState(String state) {
        this.state = state;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public UpdateProfileRequest setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public UpdateProfileRequest setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public String getHomeNumber() {
        return homeNumber;
    }

    public UpdateProfileRequest setHomeNumber(String homeNumber) {
        this.homeNumber = homeNumber;
        return this;
    }

    public String getWorkNumber() {
        return workNumber;
    }

    public UpdateProfileRequest setWorkNumber(String workNumber) {
        this.workNumber = workNumber;
        return this;
    }

    public String getOfficeNumber() {
        return officeNumber;
    }

    public UpdateProfileRequest setOfficeNumber(String officeNumber) {
        this.officeNumber = officeNumber;
        return this;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public UpdateProfileRequest setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
        return this;
    }
}

