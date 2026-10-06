package io.github.bernacamargo.iam

import io.github.bernacamargo.iam.domain.UserStatus
import io.github.bernacamargo.iam.service.IdentityService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class IdentityServiceTest {

    private val service = IdentityService()

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
}
