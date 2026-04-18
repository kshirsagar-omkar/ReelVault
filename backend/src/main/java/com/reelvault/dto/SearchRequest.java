package com.reelvault.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/search
 *
 * <p>Example payload:
 * <pre>
 * {
 *   "query": "AI agent that can browse the web",
 *   "limit": 5
 * }
 * </pre>
 */
public class SearchRequest {

    @NotBlank(message = "Search query cannot be blank")
    private String query;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit cannot exceed 50")
    private int limit = 10;

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
