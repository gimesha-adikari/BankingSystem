package com.bankingsystem.mobile.features.profile.integration.remote.dto

import com.bankingsystem.mobile.features.profile.domain.model.UserProfile

fun UserProfileDto.toDomain() = UserProfile(
    userId       = userId.orEmpty(),
    username     = username.orEmpty(),
    firstName    = firstName.orEmpty(),
    lastName     = lastName.orEmpty(),
    address      = address.orEmpty(),
    city         = city.orEmpty(),
    state        = state.orEmpty(),
    country      = country.orEmpty(),
    postalCode   = postalCode.orEmpty(),
    homeNumber   = homeNumber.orEmpty(),
    workNumber   = workNumber.orEmpty(),
    officeNumber = officeNumber.orEmpty(),
    mobileNumber = mobileNumber.orEmpty(),
    email        = email.orEmpty(),
    roleName     = roleName.orEmpty()
)
