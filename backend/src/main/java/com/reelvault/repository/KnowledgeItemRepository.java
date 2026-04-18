package com.reelvault.repository;

import com.reelvault.model.KnowledgeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for KnowledgeItem.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Semantic search uses a native SQL query with pgvector's {@code <=>}
 *       cosine distance operator. The query vector parameter is cast to
 *       {@code vector} type inline with {@code CAST(:queryVector AS vector)}.</li>
 *   <li>Filter query also uses native SQL to support the PostgreSQL-specific
 *       {@code ANY(tags)} array operator for tag filtering.</li>
 *   <li>The similarity score is computed as {@code 1 - cosine_distance} so
 *       higher values mean more similar (0.0 = no match, 1.0 = identical).</li>
 * </ul>
 */
@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, UUID> {

    // ----------------------------------------------------------------
    // Semantic similarity search
    // Returns Object[] rows: [id, title, description, original_url, source_url,
    //   content_type, tags, personal_notes, created_at, updated_at, similarity]
    // ----------------------------------------------------------------

    @Query(
        value = """
            SELECT
                id,
                title,
                description,
                original_url,
                source_url,
                content_type,
                tags,
                personal_notes,
                created_at,
                updated_at,
                1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM knowledge_items
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<Object[]> findSimilarItems(
        @Param("queryVector") String queryVector,
        @Param("limit") int limit
    );

    // ----------------------------------------------------------------
    // Filtered browse query (content type, tag, date range)
    // ----------------------------------------------------------------

    @Query(
        value = """
            SELECT *
            FROM knowledge_items
            WHERE (:contentType IS NULL OR content_type = :contentType)
              AND (:tag        IS NULL OR :tag = ANY(tags))
              AND (:dateFrom   IS NULL OR created_at >= CAST(:dateFrom AS timestamptz))
              AND (:dateTo     IS NULL OR created_at <= CAST(:dateTo   AS timestamptz))
            ORDER BY created_at DESC
            """,
        nativeQuery = true
    )
    List<KnowledgeItem> findWithFilters(
        @Param("contentType") String contentType,
        @Param("tag")         String tag,
        @Param("dateFrom")    String dateFrom,
        @Param("dateTo")      String dateTo
    );

    // ----------------------------------------------------------------
    // Recent items — for GET /api/items/recent and Telegram /recent
    // ----------------------------------------------------------------

    List<KnowledgeItem> findTop10ByOrderByCreatedAtDesc();

    // ----------------------------------------------------------------
    // Items saved in last N hours — for Telegram /digest
    // ----------------------------------------------------------------

    @Query(
        value = """
            SELECT * FROM knowledge_items
            WHERE created_at >= NOW() - CAST(:hours AS int) * INTERVAL '1 hour'
            ORDER BY created_at DESC
            """,
        nativeQuery = true
    )
    List<KnowledgeItem> findRecentByHours(@Param("hours") int hours);
}
