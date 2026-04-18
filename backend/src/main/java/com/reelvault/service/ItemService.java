package com.reelvault.service;

import com.reelvault.dto.ItemRequest;
import com.reelvault.model.KnowledgeItem;
import com.reelvault.repository.KnowledgeItemRepository;
import com.reelvault.util.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for managing KnowledgeItems.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Save new items — auto-generates embedding via EmbeddingService</li>
 *   <li>List items with optional filters (content type, tag, date range)</li>
 *   <li>Get, update, and delete items by ID</li>
 *   <li>Provide a "recent items" list for the Telegram bot</li>
 * </ul>
 */
@Service
@Transactional
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final KnowledgeItemRepository repository;
    private final EmbeddingService embeddingService;

    public ItemService(KnowledgeItemRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    // ----------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------

    /**
     * Save a new knowledge item. Auto-generates the embedding from the
     * combined text (title + description + tags + personal notes).
     *
     * @param request the item data from the API or Telegram bot
     * @return the saved entity with generated ID and timestamps
     */
    public KnowledgeItem save(ItemRequest request) {
        KnowledgeItem item = new KnowledgeItem();
        mapRequestToEntity(request, item);

        // Build combined text for embedding
        String[] tags = request.getTags() != null
                ? request.getTags().toArray(new String[0])
                : new String[0];
        String embeddingText = embeddingService.buildEmbeddingText(
                request.getTitle(),
                request.getDescription(),
                tags,
                request.getPersonalNotes()
        );

        // Generate and store the embedding
        try {
            float[] embedding = embeddingService.generateEmbedding(embeddingText);
            item.setEmbedding(VectorUtils.toVectorString(embedding));
            log.info("Generated embedding of dim {} for item: {}", embedding.length, request.getTitle());
        } catch (Exception e) {
            log.warn("Could not generate embedding for '{}': {}. Saving without embedding.",
                    request.getTitle(), e.getMessage());
            // Save without embedding — item can still be browsed but won't appear in semantic search
        }

        KnowledgeItem saved = repository.save(item);
        log.info("Saved knowledge item [id={}]: {}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Quick-save method used by the Telegram bot. Accepts the URL, title,
     * description, content type, and notes as individual parameters.
     */
    public KnowledgeItem quickSave(
            String originalUrl,
            String sourceUrl,
            String title,
            String description,
            String contentType,
            String[] tags,
            String personalNotes
    ) {
        ItemRequest request = new ItemRequest();
        request.setOriginalUrl(originalUrl);
        request.setSourceUrl(sourceUrl);
        request.setTitle(title != null ? title : originalUrl);
        request.setDescription(description);
        request.setContentType(contentType != null ? contentType : "note");
        request.setTags(tags != null ? List.of(tags) : List.of());
        request.setPersonalNotes(personalNotes);
        return save(request);
    }

    // ----------------------------------------------------------------
    // READ
    // ----------------------------------------------------------------

    /**
     * List all items, optionally filtered by content type, a single tag,
     * and/or a date range. All filter parameters are optional (pass null to skip).
     *
     * @param contentType filter by content type (e.g. "video", "article"), or null
     * @param tag         filter by a single tag (uses PostgreSQL ANY() on TEXT[]), or null
     * @param dateFrom    ISO-8601 timestamp lower bound (inclusive), or null
     * @param dateTo      ISO-8601 timestamp upper bound (inclusive), or null
     * @return filtered and sorted list of items
     */
    @Transactional(readOnly = true)
    public List<KnowledgeItem> findAll(
            String contentType,
            String tag,
            String dateFrom,
            String dateTo
    ) {
        return repository.findWithFilters(contentType, tag, dateFrom, dateTo);
    }

    /**
     * Find a single item by its UUID.
     *
     * @param id the item UUID
     * @return Optional containing the item, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<KnowledgeItem> findById(@NonNull UUID id) {
        Objects.requireNonNull(id, "Item ID must not be null");
        return repository.findById(id);
    }

    /**
     * Returns the 10 most recently saved items.
     * Used by GET /api/items/recent and the Telegram /recent command.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeItem> findRecent() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Returns items saved within the last {@code hours} hours.
     * Used by the Telegram /digest command.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeItem> findDigest(int hours) {
        return repository.findRecentByHours(hours);
    }

    // ----------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------

    /**
     * Update an existing item. Re-generates the embedding if any text
     * field that affects it (title, description, tags, notes) has changed.
     *
     * @param id      the item UUID to update
     * @param request the new field values
     * @return the updated entity
     * @throws IllegalArgumentException if the item does not exist
     */
    public KnowledgeItem update(@NonNull UUID id, ItemRequest request) {
        Objects.requireNonNull(id, "Item ID must not be null");
        KnowledgeItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));

        // Re-generate embedding only if text fields changed
        // BUG-1 fix: use Objects.equals() — String.valueOf(null) returns "null" not null
        boolean textChanged =
                !Objects.equals(request.getTitle(),         item.getTitle())        ||
                !Objects.equals(request.getDescription(),   item.getDescription())  ||
                !Objects.equals(request.getPersonalNotes(), item.getPersonalNotes());

        mapRequestToEntity(request, item);

        if (textChanged) {
            String[] tags = item.getTags() != null ? item.getTags() : new String[0];
            String embeddingText = embeddingService.buildEmbeddingText(
                    item.getTitle(), item.getDescription(), tags, item.getPersonalNotes()
            );
            try {
                float[] embedding = embeddingService.generateEmbedding(embeddingText);
                item.setEmbedding(VectorUtils.toVectorString(embedding));
            } catch (Exception e) {
                log.warn("Could not regenerate embedding on update for id={}: {}", id, e.getMessage());
            }
        }

        return repository.save(item);
    }

    // ----------------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------------

    /**
     * Delete an item by ID.
     *
     * @param id the item UUID
     * @throws IllegalArgumentException if the item does not exist
     */
    public void delete(@NonNull UUID id) {
        Objects.requireNonNull(id, "Item ID must not be null");
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Item not found: " + id);
        }
        repository.deleteById(id);
        log.info("Deleted knowledge item [id={}]", id);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private void mapRequestToEntity(ItemRequest request, KnowledgeItem item) {
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setOriginalUrl(request.getOriginalUrl());
        item.setSourceUrl(request.getSourceUrl());
        item.setContentType(request.getContentType());
        item.setPersonalNotes(request.getPersonalNotes());

        if (request.getTags() != null) {
            item.setTags(request.getTags().toArray(new String[0]));
        }
    }
}
