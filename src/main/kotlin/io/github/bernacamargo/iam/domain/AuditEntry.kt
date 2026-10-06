package io.github.bernacamargo.iam.domain

import java.time.Instant

enum class AuditAction { ACCESS_REQUESTED, ACCESS_REVOKED }

data class AuditEntry(
    val at: Instant,
    val action: AuditAction,
    val userId: String,
    val profileId: String,
    val detail: String,
)
