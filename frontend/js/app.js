/**
 * ReelVault — App Router
 *
 * Hash-based router: reads window.location.hash and shows/hides
 * the corresponding page section, updating navbar active states.
 *
 * Supported routes: #home (default), #browse, #add
 */

import { initHomePage }   from './home.js';
import { initBrowsePage } from './browse.js';
import { initAddPage }    from './add.js';

// ----------------------------------------------------------------
// Page & Nav element maps
// ----------------------------------------------------------------
const PAGES = {
    home:   document.getElementById('page-home'),
    browse: document.getElementById('page-browse'),
    add:    document.getElementById('page-add'),
};

const NAV_LINKS = {
    home:   document.getElementById('nav-home'),
    browse: document.getElementById('nav-browse'),
    add:    document.getElementById('nav-add'),
};

// Track initialised pages to avoid double inits
const initialised = new Set();

// ----------------------------------------------------------------
// Router core
// ----------------------------------------------------------------

/**
 * Navigate to a page by name ('home', 'browse', 'add').
 * Updates display, active nav link, and initialises the page module.
 * @param {string} pageName
 */
function navigateTo(pageName) {
    const validPages = Object.keys(PAGES);
    const target = validPages.includes(pageName) ? pageName : 'home';

    // Hide all pages and remove active class
    validPages.forEach(name => {
        if (PAGES[name]) {
            PAGES[name].style.display = 'none';
            // BUG-3 fix: always remove active class, not just set display
            PAGES[name].classList.remove('active');
        }
    });

    // Show target page
    if (PAGES[target]) {
        PAGES[target].style.display = '';
        PAGES[target].classList.add('active');
    }

    // Update nav links
    Object.entries(NAV_LINKS).forEach(([name, el]) => {
        if (el) el.classList.toggle('active', name === target);
    });

    // Scroll to top
    window.scrollTo({ top: 0, behavior: 'smooth' });

    // Update document title
    const titles = {
        home:   'ReelVault — Search',
        browse: 'ReelVault — Browse',
        add:    'ReelVault — Add Item',
    };
    document.title = titles[target] || 'ReelVault';

    // Lazily initialise page modules (only once)
    if (!initialised.has(target)) {
        initialised.add(target);
        switch (target) {
            case 'home':   initHomePage();   break;
            case 'browse': initBrowsePage(); break;
            case 'add':    initAddPage();    break;
        }
    }

    // On revisit to browse page, reload items
    if (target === 'browse' && initialised.has('browse')) {
        document.dispatchEvent(new CustomEvent('reelvault:browse:load'));
    }
}

// ----------------------------------------------------------------
// Hash change listener
// ----------------------------------------------------------------

function handleHashChange() {
    const hash = window.location.hash.slice(1) || 'home';
    navigateTo(hash);
}

window.addEventListener('hashchange', handleHashChange);

// ----------------------------------------------------------------
// Global toast notification utility
// ----------------------------------------------------------------

/**
 * Show a toast notification.
 * @param {string} message
 * @param {'success'|'error'|'info'} [type='info']
 * @param {number} [duration=3500] — ms before auto-dismiss
 */
export function showToast(message, type = 'info', duration = 3500) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const icons = { success: '✅', error: '❌', info: 'ℹ️' };

    const toast = document.createElement('div');
    toast.className = `toast toast--${type}`;
    toast.innerHTML = `<span>${icons[type] || 'ℹ️'}</span><span>${message}</span>`;
    container.appendChild(toast);

    // Auto-remove
    setTimeout(() => {
        toast.style.animation = 'toastOut 0.3s ease both';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ----------------------------------------------------------------
// Global item card open (modal)
// ----------------------------------------------------------------

/**
 * Open the item detail modal.
 * @param {Object} item
 */
export function openItemModal(item) {
    const overlay = document.getElementById('modal-overlay');
    const content = document.getElementById('modal-content');
    if (!overlay || !content) return;

    content.innerHTML = renderModalContent(item);
    overlay.style.display = 'flex';

    // Close handlers — MED-5 fix: use {once:true} to prevent keydown listener accumulation
    document.getElementById('modal-close').onclick = closeModal;
    overlay.onclick = (e) => { if (e.target === overlay) closeModal(); };
    document.addEventListener('keydown', escapeClose, { once: true });
}

function closeModal() {
    const overlay = document.getElementById('modal-overlay');
    if (overlay) overlay.style.display = 'none';
    document.removeEventListener('keydown', escapeClose);
}

function escapeClose(e) {
    if (e.key === 'Escape') closeModal();
}

function renderModalContent(item) {
    const tags = Array.isArray(item.tags) ? item.tags : [];
    const tagsHtml = tags.map(t => `<span class="tag-badge">${escapeHtml(t)}</span>`).join('');

    // SEC-7: Whitelist content_type before injecting into CSS class to prevent class-injection XSS
    const VALID_TYPES = new Set(['tool', 'article', 'video', 'paper', 'course', 'note']);
    const safeType = VALID_TYPES.has(item.content_type) ? item.content_type : 'unknown';
    const typeBadge = item.content_type
        ? `<span class="type-badge type-badge--${safeType}">${typeIcon(safeType)} ${escapeHtml(item.content_type)}</span>`
        : '';
    const date = item.created_at ? new Date(item.created_at).toLocaleDateString('en-US', {
        year: 'numeric', month: 'long', day: 'numeric'
    }) : '';

    return `
        <div class="modal-detail">
            <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
                ${typeBadge}
                ${date ? `<span style="font-size:0.75rem;color:var(--color-text-muted);">Saved ${escapeHtml(date)}</span>` : ''}
            </div>
            <h3 class="modal-detail__title">${escapeHtml(item.title || 'Untitled')}</h3>

            ${item.description ? `
            <div class="modal-detail__field">
                <div class="modal-detail__label">Description</div>
                <div class="modal-detail__value">${escapeHtml(item.description)}</div>
            </div>` : ''}

            ${item.original_url ? `
            <div class="modal-detail__field">
                <div class="modal-detail__label">Original URL</div>
                <div class="modal-detail__value">
                    <a href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener" class="link">
                        ${escapeHtml(item.original_url)}
                    </a>
                </div>
            </div>` : ''}

            ${item.source_url ? `
            <div class="modal-detail__field">
                <div class="modal-detail__label">Source URL</div>
                <div class="modal-detail__value">
                    <a href="${escapeHtml(item.source_url)}" target="_blank" rel="noopener" class="link">
                        ${escapeHtml(item.source_url)}
                    </a>
                </div>
            </div>` : ''}

            ${tags.length > 0 ? `
            <div class="modal-detail__field">
                <div class="modal-detail__label">Tags</div>
                <div class="card__tags" style="margin-top:6px;">${tagsHtml}</div>
            </div>` : ''}

            ${item.personal_notes ? `
            <div class="modal-detail__field">
                <div class="modal-detail__label">Personal Notes</div>
                <div class="modal-detail__value" style="white-space:pre-wrap;">${escapeHtml(item.personal_notes)}</div>
            </div>` : ''}

            <div class="modal-detail__field">
                <div class="modal-detail__label">Item ID</div>
                <div class="modal-detail__value" style="font-family:monospace;font-size:0.75rem;">${escapeHtml(item.id || '')}</div>
            </div>

            <div class="modal-detail__actions">
                ${item.original_url ? `<a href="${escapeHtml(item.original_url)}" target="_blank" rel="noopener" class="btn btn--primary">Open URL →</a>` : ''}
                <button class="btn btn--ghost" onclick="document.getElementById('modal-overlay').style.display='none'">Close</button>
            </div>
        </div>
    `;
}

// ----------------------------------------------------------------
// Shared rendering helpers (exported for use by other page modules)
// ----------------------------------------------------------------

/**
 * Escape HTML to prevent XSS.
 * @param {string} str
 * @returns {string}
 */
export function escapeHtml(str) {
    if (typeof str !== 'string') return str ?? '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Returns an emoji icon for a content type.
 * @param {string} type
 * @returns {string}
 */
export function typeIcon(type) {
    const icons = {
        tool: '🔧', article: '📰', video: '🎬',
        paper: '📄', course: '🎓', note: '📝',
    };
    return icons[type] || '📌';
}

/**
 * Format an ISO date string to a human-readable relative or absolute date.
 * @param {string} dateStr
 * @returns {string}
 */
export function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffDays = Math.floor((now - date) / 86_400_000);

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7)  return `${diffDays} days ago`;
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

// ----------------------------------------------------------------
// Initial route
// ----------------------------------------------------------------
handleHashChange();
