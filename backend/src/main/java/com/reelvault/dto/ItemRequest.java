package com.reelvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating or updating a KnowledgeItem.
 * Used by POST /api/items and PUT /api/items/{id}
 */
public class ItemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    // SEC-6: Validate URLs to allow only http/https schemes — blocks javascript:, data:, etc.
    @Pattern(
        regexp = "^(https?://.*)?$",
        message = "original_url must start with http:// or https://"
    )
    @JsonProperty("original_url")
    private String originalUrl;

    @Pattern(
        regexp = "^(https?://.*)?$",
        message = "source_url must start with http:// or https://"
    )
    @JsonProperty("source_url")
    private String sourceUrl;

    @Pattern(
        regexp = "^(tool|article|video|paper|course|note)$",
        message = "content_type must be one of: tool, article, video, paper, course, note"
    )
    @JsonProperty("content_type")
    private String contentType;

    /**
     * Tags as a list of strings. Example: ["java", "spring", "backend"]
     */
    private List<String> tags;

    @JsonProperty("personal_notes")
    private String personalNotes;

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

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

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) { this.personalNotes = personalNotes; }
}
