package com.reelvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Response DTO for semantic search results.
 *
 * <p>Returned by POST /api/search. Includes all item fields
 * plus a similarity score (0.0 – 1.0, higher is more relevant).
 */
public class SearchResultDto {

    private String id;
    private String title;
    private String description;

    @JsonProperty("original_url")
    private String originalUrl;

    @JsonProperty("source_url")
    private String sourceUrl;

    @JsonProperty("content_type")
    private String contentType;

    private List<String> tags;

    @JsonProperty("personal_notes")
    private String personalNotes;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * Cosine similarity score: 1 - cosine_distance.
     * Values range from 0.0 (no similarity) to 1.0 (identical embeddings).
     */
    private double similarity;

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public void setTagsFromArray(String[] tagArray) {
        this.tags = tagArray != null ? Arrays.asList(tagArray) : List.of();
    }

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) { this.personalNotes = personalNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
}
