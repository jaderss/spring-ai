package dev.jaderss.banking.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jaderss.banking.BankingAssistantTest;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@BankingAssistantTest
@ActiveProfiles("mcp")
class McpServerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbc;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                        .build())
                .build();
        client.initialize();
    }

    @AfterEach
    void disconnect() {
        client.close();
    }

    @Test
    void listsTheBankingTools() {
        assertThat(client.listTools().tools())
                .extracting(McpSchema.Tool::name)
                .containsExactlyInAnyOrder("getAccounts", "getBalance", "searchTransactions", "initiateTransfer");
    }

    @Test
    void readsTheConfiguredCustomersBalanceWithRealValues() {
        assertThat(text(call("getBalance", Map.of("accountNumber", "ACME-SAV-0001"))))
                .contains("ACME-SAV-0001")
                .contains("10012.5");
    }

    @Test
    void cannotReadAnotherCustomersAccount() {
        McpSchema.CallToolResult result = call("getBalance", Map.of("accountNumber", "ACME-CHK-0002"));

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("no account ACME-CHK-0002").doesNotContain("708");
    }

    @Test
    void transfersThroughMcpAreOnlyProposals() {
        McpSchema.CallToolResult result = call(
                "initiateTransfer",
                Map.of("fromAccount", "ACME-CHK-0001", "toAccount", "ACME-SAV-0001", "amount", 1, "currency", "USD"));

        assertThat(text(result)).contains("proposalId");
        assertThat(jdbc.sql("select balance from account where number = 'ACME-SAV-0001'")
                        .query(BigDecimal.class)
                        .single())
                .isEqualByComparingTo("10012.50");
    }

    private McpSchema.CallToolResult call(String tool, Map<String, Object> arguments) {
        return client.callTool(new McpSchema.CallToolRequest(tool, arguments));
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }
}
