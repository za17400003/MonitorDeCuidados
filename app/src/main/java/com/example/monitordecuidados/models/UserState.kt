package com.example.monitordecuidados.models

sealed class UserState {
    object Initial : UserState()
    object Loading : UserState()
    data class Success(val user: UserData) : UserState()
    data class Error(val message: String) : UserState()
}

data class UserData(
    val uid: String,
    val email: String?,
    val fullName: String?,
    val role: String?
)
