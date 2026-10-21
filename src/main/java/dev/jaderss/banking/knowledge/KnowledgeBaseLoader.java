package dev.jaderss.banking.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseLoader implements ApplicationRunner {

    public static final String SOURCE = "source";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseLoader.class);
    private static final String HASH = "hash";

    private final VectorStore vectorStore;
    private final JdbcClient jdbc;
    private final ResourcePatternResolver resources;

    public KnowledgeBaseLoader(VectorStore vectorStore, JdbcClient jdbc, ResourcePatternResolver resources) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.resources = resources;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<Document> sections = new ArrayList<>();
        for (Resource resource : resources.getResources("classpath:knowledge/*.md")) {
            sections.addAll(sections(resource));
        }
        Map<String, String> storedHashes = jdbc
                .sql("select id, metadata->>'hash' as hash from vector_store")
                .query((row, rowNumber) -> Map.entry(row.getString("id"), Objects.toString(row.getString(HASH), "")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<Document> changed = sections.stream()
                .filter(section -> !section.getMetadata().get(HASH).equals(storedHashes.get(section.getId())))
                .toList();
        Set<String> current = sections.stream().map(Document::getId).collect(Collectors.toSet());
        List<String> obsolete = storedHashes.keySet().stream()
                .filter(id -> !current.contains(id)
                        || changed.stream().anyMatch(doc -> doc.getId().equals(id)))
                .toList();
        if (!obsolete.isEmpty()) {
            vectorStore.delete(obsolete);
        }
        if (!changed.isEmpty()) {
            vectorStore.add(changed);
        }
        log.info("Knowledge base has {} sections, {} embedded on this start", sections.size(), changed.size());
    }

    static List<Document> sections(Resource resource) throws IOException {
        String source = Objects.requireNonNull(resource.getFilename()).replaceFirst("\\.md$", "");
        String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
        List<Document> documents = new ArrayList<>();
        for (String block : markdown.split("(?m)^## ")) {
            if (block.startsWith("# ") || block.isBlank()) {
                continue;
            }
            String heading = block.lines().findFirst().orElseThrow().strip();
            String body = block.substring(block.indexOf('\n') + 1).strip();
            String label = source + "#" + heading;
            String text = "[" + label + "]\n" + body;
            documents.add(Document.builder()
                    .id(UUID.nameUUIDFromBytes(label.getBytes(StandardCharsets.UTF_8))
                            .toString())
                    .text(text)
                    .metadata(Map.of(SOURCE, label, HASH, sha256(text)))
                    .build());
        }
        return documents;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
