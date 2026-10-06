package io.github.bernacamargo.iam.domain

enum class UserStatus { ACTIVE, SUSPENDED, PENDING_OFFBOARDING }

data class User(
    val id: String,
    val name: String,
    val email: String,
    val department: String,
    val status: UserStatus,
    val accessProfileIds: List<String> = emptyList(),
)
