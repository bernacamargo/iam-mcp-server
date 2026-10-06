package io.github.bernacamargo.iam.config

import io.github.bernacamargo.iam.tools.IdentityTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfig {

    @Bean
    fun identityToolCallbackProvider(identityTools: IdentityTools): ToolCallbackProvider =
        MethodToolCallbackProvider.builder().toolObjects(identityTools).build()
}
