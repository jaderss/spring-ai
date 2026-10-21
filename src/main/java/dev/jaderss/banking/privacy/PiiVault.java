package dev.jaderss.banking.privacy;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PiiVault {

    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("\\b[A-Z]{4}-[A-Z]{3}-\\d{4}\\b");
    private static final Pattern TOKEN = Pattern.compile("\\b(?:ACCT|NAME)_\\d+\\b");

    private final boolean enabled;
    private final List<String> names;
    private final Map<String, String> tokenByValue = new HashMap<>();
    private final Map<String, String> valueByToken = new HashMap<>();
    private int accountCount;
    private int nameCount;

    PiiVault(List<String> knownNames) {
        this(knownNames, true);
    }

    private PiiVault(List<String> knownNames, boolean enabled) {
        this.enabled = enabled;
        this.names = knownNames.stream()
                .filter(name -> !name.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public static PiiVault passThrough() {
        return new PiiVault(List.of(), false);
    }

    public synchronized String tokenize(String text) {
        if (text == null || !enabled) {
            return text;
        }
        String masked = text;
        for (String name : names) {
            if (masked.contains(name)) {
                masked = masked.replace(name, token(name, "NAME"));
            }
        }
        Matcher matcher = ACCOUNT_NUMBER.matcher(masked);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, token(matcher.group(), "ACCT"));
        }
        return matcher.appendTail(result).toString();
    }

    public synchronized String reveal(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(
                    result, Matcher.quoteReplacement(valueByToken.getOrDefault(matcher.group(), matcher.group())));
        }
        return matcher.appendTail(result).toString();
    }

    private String token(String value, String kind) {
        return tokenByValue.computeIfAbsent(value, unused -> {
            String token = kind + "_" + (kind.equals("ACCT") ? ++accountCount : ++nameCount);
            valueByToken.put(token, value);
            return token;
        });
    }
}
