package com.bankingsystem.mobile.features.profile.integration.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "userId")      val userId: String? = null,
    @Json(name = "username")    val username: String? = null,
    @Json(name = "firstName")   val firstName: String? = null,
    @Json(name = "lastName")    val lastName: String? = null,
    @Json(name = "address")     val address: String? = null,
    @Json(name = "city")        val city: String? = null,
    @Json(name = "state")       val state: String? = null,
    @Json(name = "country")     val country: String? = null,
    @Json(name = "postalCode")  val postalCode: String? = null,
    @Json(name = "homeNumber")  val homeNumber: String? = null,
    @Json(name = "workNumber")  val workNumber: String? = null,
    @Json(name = "officeNumber")val officeNumber: String? = null,
    @Json(name = "mobileNumber")val mobileNumber: String? = null,
    @Json(name = "email")       val email: String? = null,
    @Json(name = "roleName")    val roleName: String? = null
)
