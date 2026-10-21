package dev.jaderss.banking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@Import(TestcontainersConfiguration.class)
class BankingAssistantApplicationTests {

    @Test
    void contextLoads() {}
}
