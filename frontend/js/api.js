/**
 * ReelVault — API Client
 *
 * All backend calls go through this module. The base URL is read from
 * window.REELVAULT_API_URL (set in js/env.js before deploying).
 *
 * Each function returns: { data, error }
 * - data  → response payload on success
 * - error → error message string on failure
 */

/** @returns {string} The backend base URL without trailing slash */
function getBaseUrl() {
    return (window.REELVAULT_API_URL || '').replace(/\/$/, '');
}

/**
 * Internal fetch wrapper with JSON handling and unified error shape.
 * @param {string} path
 * @param {RequestInit} [options]
 * @returns {Promise<{data: any, error: string|null}>}
 */
async function request(path, options = {}) {
    const url = getBaseUrl() + path;
    try {
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            ...options,
        });

        if (response.status === 204) {
            return { data: null, error: null };
        }

        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text;
        }

        if (!response.ok) {
            const message = data?.message || data?.error || `HTTP ${response.status}`;
            return { data: null, error: message };
        }

        return { data, error: null };

    } catch (err) {
        console.error('API request failed:', err);
        return { data: null, error: err.message || 'Network error — is the backend running?' };
    }
}

// ----------------------------------------------------------------
// Items API
// ----------------------------------------------------------------

/**
 * Save a new knowledge item.
 * @param {Object} item - { title, description, original_url, source_url, content_type, tags, personal_notes }
 * @returns {Promise<{data: Object, error: string|null}>}
 */
export async function saveItem(item) {
    return request('/api/items', {
        method: 'POST',
        body: JSON.stringify(item),
    });
}

/**
 * List all items with optional filters.
 * @param {Object} [filters] - { contentType, tag, dateFrom, dateTo }
 * @returns {Promise<{data: Object[], error: string|null}>}
 */
export async function listItems(filters = {}) {
    const params = new URLSearchParams();
    if (filters.contentType) params.set('contentType', filters.contentType);
    if (filters.tag)         params.set('tag',         filters.tag);
    if (filters.dateFrom)    params.set('dateFrom',    filters.dateFrom + 'T00:00:00Z');
    if (filters.dateTo)      params.set('dateTo',      filters.dateTo   + 'T23:59:59Z');

    const query = params.toString();
    return request(`/api/items${query ? '?' + query : ''}`);
}

/**
 * Get a single item by ID.
 * @param {string} id - UUID
 * @returns {Promise<{data: Object, error: string|null}>}
 */
export async function getItem(id) {
    return request(`/api/items/${id}`);
}

/**
 * Get the 10 most recently saved items.
 * @returns {Promise<{data: Object[], error: string|null}>}
 */
export async function getRecentItems() {
    return request('/api/items/recent');
}

/**
 * Update an existing item.
 * @param {string} id - UUID
 * @param {Object} item - updated fields
 * @returns {Promise<{data: Object, error: string|null}>}
 */
export async function updateItem(id, item) {
    return request(`/api/items/${id}`, {
        method: 'PUT',
        body: JSON.stringify(item),
    });
}

/**
 * Delete an item by ID.
 * @param {string} id - UUID
 * @returns {Promise<{data: null, error: string|null}>}
 */
export async function deleteItem(id) {
    return request(`/api/items/${id}`, {
        method: 'DELETE',
    });
}

// ----------------------------------------------------------------
// Search API
// ----------------------------------------------------------------

/**
 * Semantic search across all knowledge items.
 * @param {string} query - natural language query
 * @param {number} [limit=10] - max results (1-50)
 * @returns {Promise<{data: SearchResult[], error: string|null}>}
 *
 * @typedef {Object} SearchResult
 * @property {string}   id
 * @property {string}   title
 * @property {string}   description
 * @property {string}   original_url
 * @property {string}   source_url
 * @property {string}   content_type
 * @property {string[]} tags
 * @property {string}   personal_notes
 * @property {number}   similarity  — 0.0 to 1.0
 * @property {string}   created_at
 */
export async function searchItems(query, limit = 10) {
    return request('/api/search', {
        method: 'POST',
        body: JSON.stringify({ query, limit }),
    });
}
