package io.github.bernacamargo.iam

import io.github.bernacamargo.iam.domain.AuditAction
import io.github.bernacamargo.iam.domain.UserStatus
import io.github.bernacamargo.iam.service.IdentityService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IdentityServiceTest {

    private val fixedInstant = Instant.parse("2026-10-06T12:00:00Z")
    private val service = IdentityService(Clock.fixed(fixedInstant, ZoneOffset.UTC))

    @Test
    fun `lists all users when no filter given`() {
        val users = service.listUsers()

        assertThat(users).hasSize(4)
        assertThat(users).allSatisfy { assertThat(it.id).startsWith("u-") }
    }

    @Test
    fun `filters users by status`() {
        val suspended = service.listUsers(UserStatus.SUSPENDED)

        assertThat(suspended).hasSize(1)
        assertThat(suspended.single().id).isEqualTo("u-1003")
    }

    @Test
    fun `returns summary joining user with assigned access profiles`() {
        val summary = service.userAccessSummary("u-1002")

        assertThat(summary.user.name).isEqualTo("Bruno Tavares")
        assertThat(summary.accessProfiles.map { it.id }).containsExactly("ap-001", "ap-002")
    }

    @Test
    fun `unknown user is rejected`() {
        assertThatThrownBy { service.getUser("u-9999") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("u-9999")
    }

    @Test
    fun `dangling access profile references surface as errors`() {
        assertThatThrownBy { service.userAccessSummary("u-1004") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ap-404")
    }

    @Test
    fun `lists access profiles sorted with entitlements`() {
        val profiles = service.listAccessProfiles()

        assertThat(profiles).hasSize(3)
        assertThat(profiles.first().id).isEqualTo("ap-001")
        assertThat(profiles.first().entitlements).isNotEmpty()
    }

    @Test
    fun `requestAccess grants profile to active user and audits it`() {
        val summary = service.requestAccess("u-1001", "ap-002")

        assertThat(summary.accessProfiles.map { it.id }).containsExactly("ap-001", "ap-002")

        val entries = service.listAuditEntries()
        assertThat(entries).hasSize(1)
        assertThat(entries.single().action).isEqualTo(AuditAction.ACCESS_REQUESTED)
        assertThat(entries.single().userId).isEqualTo("u-1001")
        assertThat(entries.single().profileId).isEqualTo("ap-002")
        assertThat(entries.single().at).isEqualTo(fixedInstant)
    }

    @Test
    fun `requestAccess is denied for suspended users`() {
        assertThatThrownBy { service.requestAccess("u-1003", "ap-001") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("SUSPENDED")

        assertThat(service.listAuditEntries()).isEmpty()
    }

    @Test
    fun `requestAccess is denied for users pending offboarding`() {
        assertThatThrownBy { service.requestAccess("u-1004", "ap-001") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PENDING_OFFBOARDING")
    }

    @Test
    fun `requestAccess is denied for duplicate assignments`() {
        assertThatThrownBy { service.requestAccess("u-1001", "ap-001") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("already holds")
    }

    @Test
    fun `requestAccess validates unknown user and profile`() {
        assertThatThrownBy { service.requestAccess("u-9999", "ap-001") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { service.requestAccess("u-1001", "ap-999") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `revokeAccess removes profile and audits it`() {
        service.revokeAccess("u-1002", "ap-001")

        val summary = service.userAccessSummary("u-1002")
        assertThat(summary.accessProfiles.map { it.id }).containsExactly("ap-002")

        val entries = service.listAuditEntries()
        assertThat(entries).hasSize(1)
        assertThat(entries.single().action).isEqualTo(AuditAction.ACCESS_REVOKED)
    }

    @Test
    fun `revokeAccess is denied when user does not hold the profile`() {
        assertThatThrownBy { service.revokeAccess("u-1001", "ap-003") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("does not hold")
    }

    @Test
    fun `audit entries are returned oldest first`() {
        service.requestAccess("u-1001", "ap-002")
        service.revokeAccess("u-1002", "ap-001")
        service.requestAccess("u-1002", "ap-003")

        val actions = service.listAuditEntries().map { it.action }

        assertThat(actions).containsExactly(
            AuditAction.ACCESS_REQUESTED,
            AuditAction.ACCESS_REVOKED,
            AuditAction.ACCESS_REQUESTED,
        )
    }
}
