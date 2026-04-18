package com.reelvault.controller;

import com.reelvault.dto.SearchRequest;
import com.reelvault.dto.SearchResultDto;
import com.reelvault.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for semantic search.
 *
 * <pre>
 * POST /api/search
 *
 * Request body:
 * {
 *   "query": "AI that can browse the web autonomously",
 *   "limit": 5
 * }
 *
 * Response: array of matching items with similarity score (0.0 – 1.0)
 * </pre>
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Perform a semantic search over all knowledge items.
     *
     * <p>The query is converted to a 384-dimensional embedding via Hugging Face,
     * then matched against stored item embeddings using pgvector cosine similarity.
     *
     * @param request JSON body with "query" and optional "limit" (default 10)
     * @return 200 OK with a list of matching items sorted by descending similarity
     */
    @PostMapping
    public ResponseEntity<List<SearchResultDto>> search(@Valid @RequestBody SearchRequest request) {
        List<SearchResultDto> results = searchService.search(request.getQuery(), request.getLimit());
        return ResponseEntity.ok(results);
    }
}
