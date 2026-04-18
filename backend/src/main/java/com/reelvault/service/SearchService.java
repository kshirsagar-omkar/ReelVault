package com.reelvault.service;

import com.reelvault.dto.SearchResultDto;
import com.reelvault.repository.KnowledgeItemRepository;
import com.reelvault.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic search service using pgvector cosine similarity.
 *
 * <p>Flow:
 * <ol>
 *   <li>Convert the user's natural language query to a 384-dim embedding
 *       via EmbeddingService (Hugging Face all-MiniLM-L6-v2).</li>
 *   <li>Convert the embedding float[] to the pgvector string format "[f1,f2,...]".</li>
 *   <li>Execute a native SQL cosine similarity query using pgvector's {@code <=>} operator.</li>
 *   <li>Map the raw Object[] JDBC results to {@link SearchResultDto} objects.</li>
 * </ol>
 *
 * <p>The similarity score returned is: {@code 1 - cosine_distance}
 * (1.0 = identical, 0.0 = completely unrelated).
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    // Column indices in the native query Object[] result
    // Query: SELECT id, title, description, original_url, source_url, content_type,
    //               tags, personal_notes, created_at, updated_at, similarity
    private static final int COL_ID            = 0;
    private static final int COL_TITLE         = 1;
    private static final int COL_DESCRIPTION   = 2;
    private static final int COL_ORIGINAL_URL  = 3;
    private static final int COL_SOURCE_URL    = 4;
    private static final int COL_CONTENT_TYPE  = 5;
    private static final int COL_TAGS          = 6;
    private static final int COL_PERSONAL_NOTES= 7;
    private static final int COL_CREATED_AT    = 8;
    private static final int COL_UPDATED_AT    = 9;
    private static final int COL_SIMILARITY    = 10;

    private final KnowledgeItemRepository repository;
    private final EmbeddingService embeddingService;

    public SearchService(KnowledgeItemRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    /**
     * Search the knowledge base using natural language.
     *
     * @param query  natural language query string (e.g. "AI agent that can browse the web")
     * @param limit  maximum number of results to return (1–50)
     * @return list of {@link SearchResultDto} ordered by descending similarity
     */
    public List<SearchResultDto> search(String query, int limit) {
        log.info("Semantic search: query='{}', limit={}", query, limit);

        // Step 1: Embed the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        String queryVector = VectorUtils.toVectorString(queryEmbedding);

        // Step 2: Run pgvector similarity search
        List<Object[]> rawResults = repository.findSimilarItems(queryVector, limit);
        log.info("Found {} results for query: '{}'", rawResults.size(), query);

        // Step 3: Map to DTOs
        List<SearchResultDto> results = new ArrayList<>();
        for (Object[] row : rawResults) {
            try {
                results.add(mapRow(row));
            } catch (Exception e) {
                log.warn("Failed to map search result row: {}", e.getMessage());
            }
        }
        return results;
    }

    // ----------------------------------------------------------------
    // Row mapping — handles PostgreSQL JDBC type quirks
    // ----------------------------------------------------------------

    /**
     * Map a raw Object[] from the native query result to a SearchResultDto.
     *
     * <p>Type notes from PostgreSQL JDBC driver:
     * <ul>
     *   <li>UUID columns → java.util.UUID or String depending on JDBC config</li>
     *   <li>TEXT[] columns → java.sql.Array (call .getArray() for String[])</li>
     *   <li>TIMESTAMPTZ columns → java.sql.Timestamp or java.time.OffsetDateTime</li>
     *   <li>float8 similarity → java.lang.Double</li>
     * </ul>
     */
    private SearchResultDto mapRow(Object[] row) {
        SearchResultDto dto = new SearchResultDto();

        dto.setId(row[COL_ID] != null ? row[COL_ID].toString() : null);
        dto.setTitle(stringOrNull(row[COL_TITLE]));
        dto.setDescription(stringOrNull(row[COL_DESCRIPTION]));
        dto.setOriginalUrl(stringOrNull(row[COL_ORIGINAL_URL]));
        dto.setSourceUrl(stringOrNull(row[COL_SOURCE_URL]));
        dto.setContentType(stringOrNull(row[COL_CONTENT_TYPE]));
        dto.setPersonalNotes(stringOrNull(row[COL_PERSONAL_NOTES]));
        dto.setCreatedAt(row[COL_CREATED_AT] != null ? row[COL_CREATED_AT].toString() : null);
        dto.setUpdatedAt(row[COL_UPDATED_AT] != null ? row[COL_UPDATED_AT].toString() : null);
        dto.setSimilarity(row[COL_SIMILARITY] != null ? ((Number) row[COL_SIMILARITY]).doubleValue() : 0.0);

        // BUG-8: Handle PostgreSQL TEXT[] — Hibernate native queries may return
        // either java.sql.Array OR a String like "{tag1,tag2}" depending on driver/config
        if (row[COL_TAGS] != null) {
            try {
                if (row[COL_TAGS] instanceof java.sql.Array sqlArray) {
                    dto.setTagsFromArray((String[]) sqlArray.getArray());
                } else {
                    // Fallback: parse PostgreSQL array literal string "{tag1,tag2}"
                    String raw = row[COL_TAGS].toString().trim();
                    if (raw.startsWith("{") && raw.endsWith("}")) {
                        raw = raw.substring(1, raw.length() - 1);
                        String[] tags = raw.isEmpty() ? new String[0] : raw.split(",");
                        dto.setTagsFromArray(tags);
                    } else {
                        dto.setTagsFromArray(new String[0]);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse tags array: {}", e.getMessage());
                dto.setTagsFromArray(new String[0]);
            }
        }

        return dto;
    }

    private String stringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }
}
