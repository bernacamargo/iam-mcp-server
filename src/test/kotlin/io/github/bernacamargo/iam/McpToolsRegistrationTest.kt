package io.github.bernacamargo.iam

import io.github.bernacamargo.iam.tools.IdentityTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class McpToolsRegistrationTest(
    @Autowired private val toolCallbackProvider: ToolCallbackProvider,
    @Autowired private val identityTools: IdentityTools,
) {

    @Test
    fun `context wires the identity tools as MCP tool callbacks`() {
        val callbacks = toolCallbackProvider.toolCallbacks

        assertThat(callbacks).hasSize(3)
        assertThat(identityTools).isNotNull()
    }
}
