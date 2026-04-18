/**
 * ReelVault — Add Item Page
 *
 * Handles the "Save to Vault" form:
 * - Validates required fields
 * - Builds a live tag preview as the user types
 * - POSTs to /api/items and shows success/error feedback
 */

import { saveItem }            from './api.js';
import { showToast, escapeHtml } from './app.js';

// ----------------------------------------------------------------
// Exported init function
// ----------------------------------------------------------------

export function initAddPage() {
    const form     = document.getElementById('save-form');
    const tagsInput = document.getElementById('form-tags');

    if (!form) return;

    // Live tag preview
    tagsInput?.addEventListener('input', updateTagPreview);

    // Form submit
    form.addEventListener('submit', handleSubmit);
}

// ----------------------------------------------------------------
// Form submission
// ----------------------------------------------------------------

async function handleSubmit(e) {
    e.preventDefault();
    clearFeedback();

    if (!validateForm()) return;

    const payload = buildPayload();
    setSubmitting(true);

    const { data, error } = await saveItem(payload);
    setSubmitting(false);

    if (error) {
        showErrorFeedback('Failed to save: ' + error);
        showToast('Save failed: ' + error, 'error');
        return;
    }

    showSuccessFeedback(`Item saved! ID: ${data?.id?.substring(0, 8)}…`);
    showToast('✅ Saved to your vault!', 'success');
    resetForm();
}

// ----------------------------------------------------------------
// Validation
// ----------------------------------------------------------------

function validateForm() {
    const title = document.getElementById('form-title')?.value?.trim();
    if (!title) {
        setFieldError('error-title', 'Title is required.');
        document.getElementById('form-title')?.focus();
        return false;
    }
    return true;
}

function setFieldError(errorId, message) {
    const el = document.getElementById(errorId);
    if (el) el.textContent = message;
}

function clearFieldErrors() {
    document.querySelectorAll('.form-error').forEach(el => el.textContent = '');
}

// ----------------------------------------------------------------
// Build payload from form fields
// ----------------------------------------------------------------

function buildPayload() {
    const tagsRaw = document.getElementById('form-tags')?.value || '';
    const tags = tagsRaw
        .split(',')
        .map(t => t.trim())
        .filter(t => t.length > 0);

    return {
        title:          document.getElementById('form-title')?.value?.trim() || '',
        description:    document.getElementById('form-description')?.value?.trim() || undefined,
        original_url:   document.getElementById('form-original-url')?.value?.trim() || undefined,
        source_url:     document.getElementById('form-source-url')?.value?.trim() || undefined,
        content_type:   document.getElementById('form-type')?.value              || undefined,
        tags:           tags.length > 0 ? tags : undefined,
        personal_notes: document.getElementById('form-notes')?.value?.trim()     || undefined,
    };
}

// ----------------------------------------------------------------
// Tag preview
// ----------------------------------------------------------------

function updateTagPreview() {
    const input   = document.getElementById('form-tags');
    const preview = document.getElementById('tag-preview');
    if (!input || !preview) return;

    const tags = input.value
        .split(',')
        .map(t => t.trim())
        .filter(t => t.length > 0);

    preview.innerHTML = tags
        .map(t => `<span class="tag-badge">${escapeHtml(t)}</span>`)
        .join('');
}

// ----------------------------------------------------------------
// UI state helpers
// ----------------------------------------------------------------

function setSubmitting(on) {
    const btn     = document.getElementById('btn-save');
    const btnText = btn?.querySelector('.btn-text');
    if (!btn) return;

    btn.disabled = on;
    if (btnText) btnText.textContent = on ? 'Saving… (generating AI embedding)' : 'Save to Vault';
    btn.classList.toggle('btn-loading', on);
}

function clearFeedback() {
    clearFieldErrors();
    hideElement('form-success');
    hideElement('form-error');
}

function showSuccessFeedback(text) {
    const el   = document.getElementById('form-success');
    const textEl = document.getElementById('form-success-text');
    if (textEl) textEl.textContent = text;
    if (el) el.style.display = 'flex';
}

function showErrorFeedback(text) {
    const el   = document.getElementById('form-error');
    const textEl = document.getElementById('form-error-text');
    if (textEl) textEl.textContent = text;
    if (el) el.style.display = 'flex';
}

function hideElement(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

function resetForm() {
    document.getElementById('save-form')?.reset();
    // BUG-5 fix: null guard to prevent crash if element missing
    const tagPreview = document.getElementById('tag-preview');
    if (tagPreview) tagPreview.innerHTML = '';
    // Scroll feedback into view
    document.getElementById('form-success')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}
