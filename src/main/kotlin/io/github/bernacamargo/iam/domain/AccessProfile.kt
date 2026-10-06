package io.github.bernacamargo.iam.domain

data class AccessProfile(
    val id: String,
    val name: String,
    val description: String,
    val entitlements: List<String>,
)
