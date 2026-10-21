package dev.jaderss.banking.privacy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PiiVaults {

    private final JdbcClient jdbc;
    private final Map<String, PiiVault> byConversation = new ConcurrentHashMap<>();

    public PiiVaults(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PiiVault forConversation(String conversationKey) {
        return byConversation.computeIfAbsent(conversationKey, unused -> newVault());
    }

    public PiiVault newVault() {
        return new PiiVault(
                jdbc.sql("select full_name from customer").query(String.class).list());
    }
}
