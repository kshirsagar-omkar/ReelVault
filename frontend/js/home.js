/**
 * ReelVault — Home Page (Semantic Search)
 *
 * Handles the hero search input, sends queries to POST /api/search,
 * and renders result cards with similarity score bars.
 */

import { searchItems } from './api.js';
import { showToast, openItemModal, escapeHtml, typeIcon, formatDate } from './app.js';

// ----------------------------------------------------------------
// Exported init function (called by app.js router on first visit)
// ----------------------------------------------------------------

export function initHomePage() {
    const searchInput = document.getElementById('search-input');
    const searchBtn   = document.getElementById('search-btn');
    const chipsRow    = document.getElementById('search-chips');

    if (!searchInput) return;

    // Search on Enter key
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') triggerSearch();
    });

    // Search on button click
    searchBtn?.addEventListener('click', triggerSearch);

    // Quick example chips
    chipsRow?.addEventListener('click', (e) => {
        const chip = e.target.closest('.chip');
        if (chip) {
            const query = chip.dataset.query;
            searchInput.value = query;
            searchInput.focus();
            triggerSearch();
        }
    });

    // Focus the search bar on load
    setTimeout(() => searchInput.focus(), 100);
}

// ----------------------------------------------------------------
// Core search trigger
// ----------------------------------------------------------------

async function triggerSearch() {
    const input = document.getElementById('search-input');
    const query = input?.value?.trim();

    if (!query || query.length < 2) {
        showResultsHeader(0, query);
        showGrid([]);
        return;
    }

    setLoading(true);
    hideEmptyState();
    const { data, error } = await searchItems(query, 10);
    setLoading(false);

    if (error) {
        showToast('Search failed: ' + error, 'error');
        return;
    }

    const results = data || [];
    showResultsHeader(results.length, query);

    if (results.length === 0) {
        showGrid([]);
        document.getElementById('search-empty').style.display = 'flex';
    } else {
        showGrid(results, true /* show similarity */);
    }
}

// ----------------------------------------------------------------
// Render helpers
// ----------------------------------------------------------------

function setLoading(on) {
    const loader = document.getElementById('search-loader');
    const btn    = document.getElementById('search-btn');
    if (loader) loader.style.display = on ? 'flex' : 'none';
    if (btn)   btn.disabled = on;
}

function showResultsHeader(count, query) {
    const header = document.getElementById('results-header');
    const countEl = document.getElementById('results-count');
    const queryEl = document.getElementById('results-query');

    if (!header) return;
    header.style.display = count > 0 || query ? 'flex' : 'none';
    if (countEl) countEl.textContent = `${count} result${count !== 1 ? 's' : ''}`;
    if (queryEl) queryEl.textContent = query ? `for "${query}"` : '';
}

function hideEmptyState() {
    const el = document.getElementById('search-empty');
    if (el) el.style.display = 'none';
}

/**
 * Render item cards into the results grid.
 * @param {Object[]} items - array of item or search result objects
 * @param {boolean}  showSimilarity - whether to show the similarity bar
 */
export function showGrid(items, showSimilarity = false) {
    const grid = document.getElementById('results-grid');
    if (!grid) return;

    grid.innerHTML = '';
    items.forEach((item, index) => {
        const card = buildCard(item, showSimilarity, index);
        grid.appendChild(card);
    });
}

/**
 * Build a single item card DOM element.
 * @param {Object}  item
 * @param {boolean} showSimilarity
 * @param {number}  index
 * @returns {HTMLElement}
 */
export function buildCard(item, showSimilarity = false, index = 0) {
    const tags = Array.isArray(item.tags) ? item.tags : [];
    const similarity = typeof item.similarity === 'number' ? item.similarity : null;
    const pct = similarity !== null ? Math.round(similarity * 100) : null;

    // SEC-7: Whitelist content_type before injecting into CSS class
    const VALID_TYPES = new Set(['tool', 'article', 'video', 'paper', 'course', 'note']);
    const safeType = VALID_TYPES.has(item.content_type) ? item.content_type : 'unknown';

    const card = document.createElement('article');
    card.className = 'item-card';
    card.setAttribute('role', 'listitem');
    card.dataset.itemId = item.id;
    card.style.animationDelay = `${Math.min(index * 50, 500)}ms`;
    card.title = 'Click to view details';

    card.innerHTML = `
        <div class="card__header">
            <h3 class="card__title">${escapeHtml(item.title || 'Untitled')}</h3>
            ${item.content_type ? `<span class="type-badge type-badge--${safeType}">${typeIcon(safeType)} ${escapeHtml(item.content_type)}</span>` : ''}
        </div>

        ${item.description ? `<p class="card__description">${escapeHtml(item.description)}</p>` : ''}

        ${showSimilarity && pct !== null ? `
        <div class="card__similarity" title="Relevance score: ${pct}%">
            <div class="similarity-bar" aria-label="Similarity ${pct}%">
                <div class="similarity-bar__fill" style="width:${pct}%"></div>
            </div>
            <span class="similarity-score">${pct}%</span>
        </div>` : ''}

        ${item.original_url ? `
        <a class="card__url" href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener" onclick="event.stopPropagation()">
            <span class="card__url-icon">🔗</span>
            <span>${escapeHtml(truncateUrl(item.original_url))}</span>
        </a>` : ''}

        ${tags.length > 0 ? `
        <div class="card__tags" aria-label="Tags">
            ${tags.slice(0, 5).map(t => `<span class="tag-badge">${escapeHtml(t)}</span>`).join('')}
            ${tags.length > 5 ? `<span class="tag-badge">+${tags.length - 5}</span>` : ''}
        </div>` : ''}

        <div class="card__footer">
            <span class="card__date">${formatDate(item.created_at)}</span>
            <div class="card__actions">
                ${item.original_url ? `
                <a class="card__action-btn" href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener" title="Open URL">
                    <span>↗</span> Open
                </a>` : ''}
                <button class="card__action-btn card__action-btn--delete" data-id="${escapeHtml(item.id)}" title="Delete item">
                    🗑
                </button>
            </div>
        </div>
    `;

    // Click card → open detail modal
    card.addEventListener('click', () => openItemModal(item));

    // BUG-4 fix: Attach delete handler in buildCard so search result cards also support delete
    const deleteBtn = card.querySelector('.card__action-btn--delete');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const confirmed = window.confirm(`Delete "${item.title || 'this item'}"?\n\nThis cannot be undone.`);
            if (!confirmed) return;
            const { deleteItem } = await import('./api.js');
            const { error } = await deleteItem(item.id);
            if (error) {
                const { showToast } = await import('./app.js');
                showToast('Delete failed: ' + error, 'error');
            } else {
                card.style.transition = 'all 0.3s ease';
                card.style.opacity = '0';
                card.style.transform = 'scale(0.95)';
                setTimeout(() => card.remove(), 300);
                const { showToast } = await import('./app.js');
                showToast('Item deleted', 'success');
            }
        });
    }

    // Stop open-URL link from also opening modal
    card.querySelectorAll('a').forEach(a => a.addEventListener('click', e => e.stopPropagation()));

    return card;
}

// ----------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------

function truncateUrl(url) {
    try {
        const u = new URL(url);
        return u.hostname + (u.pathname.length > 1 ? u.pathname.substring(0, 30) + '…' : '');
    } catch {
        return url.substring(0, 40) + '…';
    }
}
