package io.github.bernacamargo.iam.tools

import io.github.bernacamargo.iam.domain.AccessProfile
import io.github.bernacamargo.iam.domain.AuditEntry
import io.github.bernacamargo.iam.domain.User
import io.github.bernacamargo.iam.domain.UserStatus
import io.github.bernacamargo.iam.service.IdentityService
import io.github.bernacamargo.iam.service.UserAccessSummary
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class IdentityTools(private val identityService: IdentityService) {

    @Tool(description = "List workforce users known to the identity platform, optionally filtered by account status")
    fun listUsers(
        @ToolParam(description = "Optional status filter: ACTIVE, SUSPENDED or PENDING_OFFBOARDING", required = false)
        status: UserStatus? = null,
    ): List<User> = identityService.listUsers(status)

    @Tool(description = "Get one workforce user by id, including the access profiles assigned to them")
    fun getUser(
        @ToolParam(description = "Unique user id, e.g. u-1001")
        userId: String,
    ): UserAccessSummary = identityService.userAccessSummary(userId)

    @Tool(description = "List all access profiles with their entitlements")
    fun listAccessProfiles(): List<AccessProfile> = identityService.listAccessProfiles()

    @Tool(description = "Grant an access profile to an ACTIVE user. Fails for suspended users, pending offboarding, duplicates, or unknown ids")
    fun requestAccess(
        @ToolParam(description = "Unique user id, e.g. u-1001")
        userId: String,
        @ToolParam(description = "Access profile id to grant, e.g. ap-002")
        profileId: String,
    ): UserAccessSummary = identityService.requestAccess(userId, profileId)

    @Tool(description = "Revoke an access profile currently held by a user")
    fun revokeAccess(
        @ToolParam(description = "Unique user id, e.g. u-1001")
        userId: String,
        @ToolParam(description = "Access profile id to revoke, e.g. ap-002")
        profileId: String,
    ): UserAccessSummary = identityService.revokeAccess(userId, profileId)

    @Tool(description = "List the audit trail of access grants and revocations, oldest first")
    fun listAuditEntries(): List<AuditEntry> = identityService.listAuditEntries()
}
