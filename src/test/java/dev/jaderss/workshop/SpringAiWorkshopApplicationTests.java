package dev.jaderss.workshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class SpringAiWorkshopApplicationTests {

    @Test
    void contextLoads() {}
}
