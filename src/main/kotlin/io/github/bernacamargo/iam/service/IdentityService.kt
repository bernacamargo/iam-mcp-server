package io.github.bernacamargo.iam.service

import io.github.bernacamargo.iam.domain.AccessProfile
import io.github.bernacamargo.iam.domain.AuditAction
import io.github.bernacamargo.iam.domain.AuditEntry
import io.github.bernacamargo.iam.domain.User
import io.github.bernacamargo.iam.domain.UserStatus
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import org.springframework.stereotype.Service

data class UserAccessSummary(val user: User, val accessProfiles: List<AccessProfile>)

/**
 * In-memory identity store backing the MCP tools. Deliberately simple:
 * the point of this project is the MCP integration, not persistence.
 *
 * Business rules live here, not in the tool layer: agents get a typed,
 * policy-enforced surface instead of raw store access.
 */
@Service
class IdentityService(private val clock: Clock = Clock.systemUTC()) {

    private val users = ConcurrentHashMap<String, User>()
    private val accessProfiles = ConcurrentHashMap<String, AccessProfile>()
    private val auditLog = ConcurrentLinkedDeque<AuditEntry>()

    init {
        seed()
    }

    fun listUsers(status: UserStatus? = null): List<User> =
        users.values
            .filter { status == null || it.status == status }
            .sortedBy { it.id }

    fun getUser(userId: String): User =
        users[userId] ?: throw IllegalArgumentException("Unknown user: $userId")

    fun listAccessProfiles(): List<AccessProfile> =
        accessProfiles.values.sortedBy { it.id }

    fun getAccessProfile(profileId: String): AccessProfile =
        accessProfiles[profileId] ?: throw IllegalArgumentException("Unknown access profile: $profileId")

    fun userAccessSummary(userId: String): UserAccessSummary {
        val user = getUser(userId)
        val profiles = user.accessProfileIds.map { getAccessProfile(it) }
        return UserAccessSummary(user, profiles)
    }

    fun listAuditEntries(): List<AuditEntry> = auditLog.toList()

    /**
     * Grants an access profile to a user.
     * Policy: only ACTIVE users receive access, and assignments are unique.
     */
    fun requestAccess(userId: String, profileId: String): UserAccessSummary {
        val user = requireUser(userId)
        getAccessProfile(profileId) // validate existence up front

        if (user.status != UserStatus.ACTIVE) {
            throw IllegalStateException(
                "Access request denied: user $userId is ${user.status}, only ACTIVE users can receive access",
            )
        }
        if (profileId in user.accessProfileIds) {
            throw IllegalStateException("Access request denied: user $userId already holds $profileId")
        }

        val updated = user.copy(accessProfileIds = user.accessProfileIds + profileId)
        users[userId] = updated
        audit(userId, profileId, AuditAction.ACCESS_REQUESTED, "granted")
        return userAccessSummary(userId)
    }

    /** Removes an access profile from a user. Policy: the assignment must exist. */
    fun revokeAccess(userId: String, profileId: String): UserAccessSummary {
        val user = requireUser(userId)

        if (profileId !in user.accessProfileIds) {
            throw IllegalStateException("Revoke denied: user $userId does not hold $profileId")
        }

        users[userId] = user.copy(accessProfileIds = user.accessProfileIds - profileId)
        audit(userId, profileId, AuditAction.ACCESS_REVOKED, "revoked")
        return userAccessSummary(userId)
    }

    private fun requireUser(userId: String): User =
        users[userId] ?: throw IllegalArgumentException("Unknown user: $userId")

    private fun audit(userId: String, profileId: String, action: AuditAction, detail: String) {
        auditLog.addLast(
            AuditEntry(at = clock.instant(), action = action, userId = userId, profileId = profileId, detail = detail),
        )
    }

    private fun seed() {
        accessProfiles["ap-001"] = AccessProfile(
            id = "ap-001",
            name = "finance-app-read",
            description = "Read-only access to the finance reporting app",
            entitlements = listOf("finance://reports/read"),
        )
        accessProfiles["ap-002"] = AccessProfile(
            id = "ap-002",
            name = "hr-portal-edit",
            description = "Edit records in the HR portal",
            entitlements = listOf("hr://employees/write", "hr://org-chart/read"),
        )
        accessProfiles["ap-003"] = AccessProfile(
            id = "ap-003",
            name = "admin-console-full",
            description = "Full administrative console access — requires VP approval",
            entitlements = listOf("admin://console/*"),
        )

        users["u-1001"] = User(
            id = "u-1001",
            name = "Ana Ribeiro",
            email = "ana.ribeiro@example.com",
            department = "Engineering",
            status = UserStatus.ACTIVE,
            accessProfileIds = listOf("ap-001"),
        )
        users["u-1002"] = User(
            id = "u-1002",
            name = "Bruno Tavares",
            email = "bruno.tavares@example.com",
            department = "Finance",
            status = UserStatus.ACTIVE,
            accessProfileIds = listOf("ap-001", "ap-002"),
        )
        users["u-1003"] = User(
            id = "u-1003",
            name = "Carla Menezes",
            email = "carla.menezes@example.com",
            department = "Human Resources",
            status = UserStatus.SUSPENDED,
            accessProfileIds = listOf("ap-002"),
        )
        users["u-1004"] = User(
            id = "u-1004",
            name = "Diego Fontes",
            email = "diego.fontes@example.com",
            department = "Engineering",
            status = UserStatus.PENDING_OFFBOARDING,
            // Dangling reference on purpose: identity data integrity issues must surface loudly
            accessProfileIds = listOf("ap-404"),
        )
    }
}
