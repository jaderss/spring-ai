package dev.jaderss.banking;

import dev.jaderss.banking.assistant.ScriptedAssistantConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.ai.model.chat=none",
            "spring.ai.model.embedding=none",
            "spring.ai.model.embedding.text=none",
            "spring.ai.openai.api-key=test-key"
        })
@Import(ScriptedAssistantConfiguration.class)
public @interface BankingAssistantTest {}
