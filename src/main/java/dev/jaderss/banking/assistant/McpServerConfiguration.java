package dev.jaderss.banking.assistant;

import dev.jaderss.banking.core.CustomerId;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "banking.mcp", name = "customer-id")
class McpServerConfiguration {

    @Bean
    ToolCallbackProvider bankingMcpTools(BankingTools tools, @Value("${banking.mcp.customer-id}") String customerId) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(new McpBankingTools(tools, new CustomerId(customerId)))
                .build();
    }
}
