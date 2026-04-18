package com.reelvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service that generates 384-dimensional sentence embeddings by calling
 * the Hugging Face Inference API with the {@code sentence-transformers/all-MiniLM-L6-v2} model.
 *
 * <p>API endpoint:
 * POST https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2
 *
 * <p>Request body: {@code {"inputs": "your text here"}}
 *
 * <p>Response format variations (we handle all cases):
 * <ol>
 *   <li>1D float array {@code [f1, f2, ...]} — already pooled (384 values), use directly.</li>
 *   <li>2D array {@code [[f1, f2, ...]]} — one sentence, one embedding row, use row 0.</li>
 *   <li>2D token-level array {@code [[tok1_e1,...], [tok2_e1,...]]} — mean pool over tokens.</li>
 * </ol>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${app.huggingface.api-key}")
    private String apiKey;

    @Value("${app.huggingface.api-url}")
    private String apiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Generate a 384-dimensional embedding for the given text.
     *
     * @param text the input text (title + description + tags + notes combined)
     * @return float[] of length 384
     * @throws RuntimeException if the API call fails or model is loading
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cannot generate embedding for blank text");
        }
        // MED-1: Truncate to ~500 tokens (2000 chars) to avoid HuggingFace silent truncation
        if (text.length() > 2000) {
            log.debug("Truncating embedding text from {} to 2000 chars", text.length());
            text = text.substring(0, 2000);
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("inputs", text));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 503) {
                // Model is still loading on HuggingFace free tier — retry after brief wait
                log.warn("HuggingFace model loading (503). Waiting 20s then retrying...");
                Thread.sleep(20_000);
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                // SEC-2: Truncate body in log to prevent API key reflection in error messages
                String safeBody = response.body();
                if (safeBody != null && safeBody.length() > 200) safeBody = safeBody.substring(0, 200) + "...";
                throw new RuntimeException(
                    "HuggingFace API error [" + response.statusCode() + "]: " + safeBody
                );
            }

            return parseEmbedding(response.body());

        } catch (IOException e) {
            // BUG-2: Separate IO errors from interrupt handling
            throw new RuntimeException("Failed to call HuggingFace embedding API (IO error)", e);
        } catch (InterruptedException e) {
            // BUG-2: Restore interrupt flag BEFORE throwing so callers can react properly
            Thread.currentThread().interrupt();
            throw new RuntimeException("HuggingFace API call interrupted", e);
        }
    }

    /**
     * Parse the HuggingFace API response body into a float[] embedding.
     * Handles 1D, 2D (single-sentence), and 2D (token-level) response formats.
     */
    private float[] parseEmbedding(String responseBody) throws IOException {
        // Parse into a generic nested List structure using Jackson
        List<Object> parsed = objectMapper.readValue(responseBody, new TypeReference<>() {});

        if (parsed.isEmpty()) {
            throw new RuntimeException("Empty embedding response from HuggingFace API");
        }

        Object first = parsed.get(0);

        if (first instanceof Number) {
            // Case 1: 1D array [f1, f2, ...] — sentence is already pooled
            log.debug("Embedding response: 1D array of {} values", parsed.size());
            return toFloatArray(parsed);

        } else if (first instanceof List<?> innerList) {
            if (!innerList.isEmpty() && innerList.get(0) instanceof Number) {
                // Case 2: 2D array [[f1, f2, ...]] — one row per sentence, take row 0
                log.debug("Embedding response: 2D array [{} rows x {} dims]",
                        parsed.size(), innerList.size());
                if (parsed.size() == 1) {
                    return toFloatArray(innerList);
                }
                // Multiple sentences returned (shouldn't happen for single input)
                // Mean pool all rows
                return meanPool(castToDoubleList(parsed));

            } else if (!innerList.isEmpty() && innerList.get(0) instanceof List) {
                // Case 3: 3D array — token-level embeddings, mean pool over tokens
                log.debug("Embedding response: 3D token-level array, applying mean pooling");
                @SuppressWarnings("unchecked")
                List<List<Object>> tokenEmbeddings = (List<List<Object>>) (List<?>) innerList;
                return meanPool(tokenEmbeddings);
            }
        }

        throw new RuntimeException("Unrecognised embedding response format from HuggingFace");
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private float[] toFloatArray(List<?> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = ((Number) list.get(i)).floatValue();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<List<Object>> castToDoubleList(List<Object> list) {
        return (List<List<Object>>) (List<?>) list;
    }

    private float[] meanPool(List<List<Object>> tokenEmbeddings) {
        if (tokenEmbeddings.isEmpty()) return new float[0];
        int dim = tokenEmbeddings.get(0).size();
        double[] sum = new double[dim];

        for (List<Object> tokenEmb : tokenEmbeddings) {
            for (int j = 0; j < dim; j++) {
                sum[j] += ((Number) tokenEmb.get(j)).doubleValue();
            }
        }

        float[] mean = new float[dim];
        for (int j = 0; j < dim; j++) {
            mean[j] = (float) (sum[j] / tokenEmbeddings.size());
        }
        return mean;
    }

    /**
     * Build the combined text string used for embedding generation.
     * Concatenates title, description, tags, and personal notes.
     *
     * @param title        item title
     * @param description  item description
     * @param tags         tag array
     * @param personalNotes personal notes
     * @return combined string for embedding
     */
    public String buildEmbeddingText(
            String title,
            String description,
            String[] tags,
            String personalNotes
    ) {
        StringBuilder sb = new StringBuilder();
        if (title != null)         sb.append(title).append(". ");
        if (description != null)   sb.append(description).append(". ");
        if (tags != null && tags.length > 0) {
            sb.append("Tags: ").append(String.join(", ", tags)).append(". ");
        }
        if (personalNotes != null) sb.append(personalNotes);
        return sb.toString().trim();
    }
}
