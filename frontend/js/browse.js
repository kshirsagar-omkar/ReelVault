/**
 * ReelVault — Browse Page
 *
 * Lists all saved knowledge items with filter controls:
 * content type, tag, date-from, date-to.
 */

import { listItems, deleteItem } from './api.js';
import { showToast, escapeHtml } from './app.js';
import { buildCard }             from './home.js';

// ----------------------------------------------------------------
// Exported init function
// ----------------------------------------------------------------

export function initBrowsePage() {
    // Wire up filter / clear buttons
    document.getElementById('btn-apply-filter')?.addEventListener('click', loadItems);
    document.getElementById('btn-clear-filter')?.addEventListener('click', clearFilters);

    // Re-load when the router dispatches the reload event
    document.addEventListener('reelvault:browse:load', loadItems);

    // Handle delete clicks via event delegation on the browse grid
    document.getElementById('browse-grid')?.addEventListener('click', handleGridClick);

    // Initial load
    loadItems();
}

// ----------------------------------------------------------------
// Load & render items
// ----------------------------------------------------------------

async function loadItems() {
    setLoading(true);
    setEmptyState(false);

    const filters = getFilters();
    const { data, error } = await listItems(filters);

    setLoading(false);

    if (error) {
        showToast('Could not load items: ' + error, 'error');
        return;
    }

    const items = data || [];
    updateStats(items.length, filters);
    renderGrid(items);
}

function renderGrid(items) {
    const grid = document.getElementById('browse-grid');
    if (!grid) return;

    grid.innerHTML = '';

    if (items.length === 0) {
        setEmptyState(true);
        return;
    }

    items.forEach((item, index) => {
        // buildCard attaches its own delete handler (BUG-4/LOW-4 fix)
        // No additional handler needed here; rely on buildCard's internal handler
        const card = buildCard(item, false, index);
        grid.appendChild(card);
    });
}

// ----------------------------------------------------------------
// Delete
// ----------------------------------------------------------------

async function confirmDelete(id, title, cardEl) {
    const confirmed = window.confirm(
        `Delete "${title || 'this item'}"?\n\nThis cannot be undone.`
    );
    if (!confirmed) return;

    const { error } = await deleteItem(id);
    if (error) {
        showToast('Delete failed: ' + error, 'error');
        return;
    }

    // Animate card out
    cardEl.style.animation = 'none';
    cardEl.style.transition = 'all 0.3s ease';
    cardEl.style.opacity = '0';
    cardEl.style.transform = 'scale(0.95)';
    setTimeout(() => cardEl.remove(), 300);

    showToast('Item deleted', 'success');
}

/**
 * Handle delete clicks on the browse grid via event delegation.
 * This catches delete buttons on cards that were rendered before
 * event listeners were attached.
 */
function handleGridClick(e) {
    const deleteBtn = e.target.closest('.card__action-btn--delete');
    if (!deleteBtn) return;
    e.stopPropagation();

    const card = deleteBtn.closest('.item-card');
    const id   = deleteBtn.dataset.id || card?.dataset?.itemId;
    const title = card?.querySelector('.card__title')?.textContent;

    if (id) confirmDelete(id, title, card);
}

// ----------------------------------------------------------------
// Filter helpers
// ----------------------------------------------------------------

function getFilters() {
    return {
        contentType: document.getElementById('filter-type')?.value  || '',
        tag:         document.getElementById('filter-tag')?.value.trim()  || '',
        dateFrom:    document.getElementById('filter-date-from')?.value   || '',
        dateTo:      document.getElementById('filter-date-to')?.value     || '',
    };
}

function clearFilters() {
    ['filter-type', 'filter-tag', 'filter-date-from', 'filter-date-to'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    loadItems();
}

// ----------------------------------------------------------------
// UI state helpers
// ----------------------------------------------------------------

function setLoading(on) {
    const loader = document.getElementById('browse-loader');
    if (loader) loader.style.display = on ? 'flex' : 'none';
}

function setEmptyState(on) {
    const el = document.getElementById('browse-empty');
    if (el) el.style.display = on ? 'flex' : 'none';
}

function updateStats(count, filters) {
    const statsEl = document.getElementById('browse-stats');
    if (!statsEl) return;

    const activeFilters = Object.values(filters).filter(Boolean).length;
    const filterNote = activeFilters > 0 ? ` (${activeFilters} filter${activeFilters > 1 ? 's' : ''} active)` : '';
    statsEl.textContent = `${count} item${count !== 1 ? 's' : ''}${filterNote}`;
}
