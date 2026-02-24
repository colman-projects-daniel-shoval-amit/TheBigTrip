package com.dsa.thebigtrip.data.user

data class UserDto(
    val uid: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    var imageUri: String? = null
)