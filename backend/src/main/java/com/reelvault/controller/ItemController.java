package com.reelvault.controller;

import com.reelvault.dto.ItemRequest;
import com.reelvault.model.KnowledgeItem;
import com.reelvault.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for CRUD operations on KnowledgeItems.
 *
 * <p>All endpoints are prefixed with {@code /api/items}.
 *
 * <pre>
 * POST   /api/items              Save a new item (auto-generates embedding)
 * GET    /api/items              List all items with optional filters
 * GET    /api/items/recent       Last 10 saved items
 * GET    /api/items/{id}         Get a single item by UUID
 * PUT    /api/items/{id}         Update an item
 * DELETE /api/items/{id}         Delete an item
 * </pre>
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // ----------------------------------------------------------------
    // POST /api/items — Save a new item
    // ----------------------------------------------------------------

    /**
     * Save a new knowledge item. The embedding is generated automatically
     * from the combined title + description + tags + personalNotes.
     *
     * @param request JSON body with item fields
     * @return 201 Created with the saved item
     */
    @PostMapping
    public ResponseEntity<KnowledgeItem> createItem(@Valid @RequestBody ItemRequest request) {
        KnowledgeItem saved = itemService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ----------------------------------------------------------------
    // GET /api/items — List all with filters
    // ----------------------------------------------------------------

    /**
     * List all knowledge items with optional query parameters for filtering.
     *
     * @param contentType filter by type (tool|article|video|paper|course|note)
     * @param tag         filter by a single tag (case-sensitive)
     * @param dateFrom    ISO-8601 dateTime lower bound, e.g. 2024-01-01T00:00:00Z
     * @param dateTo      ISO-8601 dateTime upper bound
     * @return 200 OK with list of items
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeItem>> listItems(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        List<KnowledgeItem> items = itemService.findAll(contentType, tag, dateFrom, dateTo);
        return ResponseEntity.ok(items);
    }

    // ----------------------------------------------------------------
    // GET /api/items/recent — Last 10 items
    // ----------------------------------------------------------------

    /**
     * Returns the 10 most recently saved items, ordered by created_at descending.
     * Also used by the Telegram bot's /recent command.
     *
     * @return 200 OK with list of up to 10 items
     */
    @GetMapping("/recent")
    public ResponseEntity<List<KnowledgeItem>> getRecentItems() {
        return ResponseEntity.ok(itemService.findRecent());
    }

    // ----------------------------------------------------------------
    // GET /api/items/{id} — Single item
    // ----------------------------------------------------------------

    /**
     * Get a single knowledge item by its UUID.
     *
     * @param id the item UUID
     * @return 200 OK with the item, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeItem> getItem(@PathVariable @NonNull UUID id) {
        return itemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ----------------------------------------------------------------
    // PUT /api/items/{id} — Update item
    // ----------------------------------------------------------------

    /**
     * Update an existing item. Re-generates the embedding if any text field changed.
     *
     * @param id      the item UUID
     * @param request JSON body with updated fields
     * @return 200 OK with updated item, or 404 Not Found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody ItemRequest request
    ) {
        try {
            KnowledgeItem updated = itemService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ----------------------------------------------------------------
    // DELETE /api/items/{id} — Delete item
    // ----------------------------------------------------------------

    /**
     * Delete a knowledge item.
     *
     * @param id the item UUID
     * @return 204 No Content on success, 404 Not Found if item doesn't exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable @NonNull UUID id) {
        try {
            itemService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
