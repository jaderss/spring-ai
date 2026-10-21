package dev.jaderss.banking.assistant;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

public class FakeEmbeddingModel implements EmbeddingModel {

    static final int DIMENSIONS = 1536;

    private final AtomicInteger embeddedTexts = new AtomicInteger();

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        embeddedTexts.addAndGet(texts.size());
        return new EmbeddingResponse(texts.stream()
                .map(text -> new Embedding(vector(text), texts.indexOf(text)))
                .toList());
    }

    @Override
    public float[] embed(Document document) {
        embeddedTexts.incrementAndGet();
        return vector(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    public int embeddedTexts() {
        return embeddedTexts.get();
    }

    static float[] vector(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.length() > 2) {
                vector[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1;
            }
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        float scale = norm == 0 ? 0 : (float) (1 / Math.sqrt(norm));
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] *= scale;
        }
        return vector;
    }
}
