package dev.jaderss.workshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@Import(TestcontainersConfiguration.class)
class SpringAiWorkshopApplicationTests {

    @Test
    void contextLoads() {}
}
