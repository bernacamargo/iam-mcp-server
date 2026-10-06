package io.github.bernacamargo.iam.service

import io.github.bernacamargo.iam.domain.AccessProfile
import io.github.bernacamargo.iam.domain.User
import io.github.bernacamargo.iam.domain.UserStatus
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

data class UserAccessSummary(val user: User, val accessProfiles: List<AccessProfile>)

/**
 * In-memory identity store backing the MCP tools. Deliberately simple:
 * the point of this project is the MCP integration, not persistence.
 */
@Service
class IdentityService {

    private val users = ConcurrentHashMap<String, User>()
    private val accessProfiles = ConcurrentHashMap<String, AccessProfile>()

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
