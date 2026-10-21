package dev.jaderss.banking.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.jaderss.banking.BankingAssistantTest;
import dev.jaderss.banking.assistant.FakeEmbeddingModel;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

@BankingAssistantTest
class KnowledgeBaseTest {

    @Autowired
    private KnowledgeBaseLoader loader;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private FakeEmbeddingModel embeddingModel;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void splitsDocumentsIntoLabelledSections() throws IOException {
        assertThat(KnowledgeBaseLoader.sections(new ClassPathResource("knowledge/fee-schedule.md")))
                .extracting(document -> document.getMetadata().get(KnowledgeBaseLoader.SOURCE))
                .containsExactly(
                        "fee-schedule#Monthly maintenance",
                        "fee-schedule#Overdraft",
                        "fee-schedule#Transfers",
                        "fee-schedule#Foreign transactions",
                        "fee-schedule#Paper statements");
    }

    @Test
    void startupStoresEverySectionOnce() {
        assertThat(jdbc.sql("select count(*) from vector_store")
                        .query(Long.class)
                        .single())
                .isEqualTo(16L);
    }

    @Test
    void reloadingUnchangedDocumentsEmbedsNothing() throws Exception {
        int before = embeddingModel.embeddedTexts();

        loader.run(null);

        assertThat(embeddingModel.embeddedTexts()).isEqualTo(before);
        assertThat(jdbc.sql("select count(*) from vector_store")
                        .query(Long.class)
                        .single())
                .isEqualTo(16L);
    }

    @Test
    void retrievesTheMatchingSection() {
        assertThat(vectorStore.similaritySearch(SearchRequest.builder()
                        .query("how much do wire transfers cost")
                        .topK(1)
                        .build()))
                .extracting(Document::getMetadata)
                .extracting(metadata -> metadata.get(KnowledgeBaseLoader.SOURCE))
                .containsExactly("fee-schedule#Transfers");
    }
}
