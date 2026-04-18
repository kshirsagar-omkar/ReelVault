package com.reelvault.service;

import com.reelvault.dto.SearchResultDto;
import com.reelvault.model.KnowledgeItem;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.InetAddress;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Telegram bot for ReelVault — saves links and searches the knowledge base from Telegram.
 *
 * <p>Supported commands:
 * <pre>
 * /save [url]              — Auto-fetch title from URL and save
 * /save [url] - [note]     — Save URL with a personal note
 * /search [query]          — Semantic search, returns top 5 matches
 * /recent                  — Shows last 5 saved items
 * /digest                  — Items saved in the last 24 hours
 * /list [tag]              — Filter items by a single tag
 * /help                    — Show all available commands
 * </pre>
 *
 * <p>The bot registers itself on startup via {@link #registerBot()}.
 * It runs inside the Spring Boot application on Railway — no separate hosting needed.
 */
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    // SEC-4: Allowlist of authorized Telegram chat IDs.
    // Set TELEGRAM_ALLOWED_CHAT_ID in Railway env vars (your personal Telegram user ID).
    // Leave empty string to allow all users (NOT recommended for production).
    @Value("${app.telegram.allowed-chat-id:}")
    private String allowedChatId;

    @Value("${app.telegram.bot-username}")
    private String botUsername;

    private final ItemService itemService;
    private final SearchService searchService;

    public TelegramBotService(
            @Value("${app.telegram.bot-token}") String botToken,
            ItemService itemService,
            SearchService searchService
    ) {
        super(botToken);
        this.itemService = itemService;
        this.searchService = searchService;
    }

    // ----------------------------------------------------------------
    // Bot registration on startup
    // ----------------------------------------------------------------

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("Telegram bot registered: @{}", botUsername);
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    // ----------------------------------------------------------------
    // Message handling
    // ----------------------------------------------------------------

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String chatId  = update.getMessage().getChatId().toString();
        String text    = update.getMessage().getText().trim();
        String command = extractCommand(text);
        String args    = extractArgs(text);

        // SEC-4: Reject unauthorized users
        if (allowedChatId != null && !allowedChatId.isBlank() && !allowedChatId.equals(chatId)) {
            log.warn("Unauthorized Telegram access attempt from chatId={}", chatId);
            sendText(chatId, "\u26d4 Unauthorized. This is a private bot.");
            return;
        }

        log.info("Bot received [chatId={}]: {}", chatId, text);

        String response = switch (command) {
            case "/save"   -> handleSave(args);
            case "/search" -> handleSearch(args);
            case "/recent" -> handleRecent();
            case "/digest" -> handleDigest();
            case "/list"   -> handleList(args);
            case "/help"   -> helpMessage();
            case "/start"  -> "\uD83D\uDC4B Welcome to *ReelVault*!\n\n" + helpMessage();
            default        -> "\u2753 Unknown command. Type /help for a list of commands.";
        };

        sendText(chatId, response);
    }

    // ----------------------------------------------------------------
    // Command handlers
    // ----------------------------------------------------------------

    /**
     * /save [url] — Save a URL, auto-fetching its title and description via Jsoup.
     * /save [url] - [note] — Save a URL with a personal note.
     */
    private String handleSave(String args) {
        if (args.isBlank()) {
            return "⚠️ Usage:\n`/save https://example.com`\n`/save https://example.com - My note here`";
        }

        String url;
        String note = null;

        // Check for "url - note" pattern
        int separatorIdx = args.indexOf(" - ");
        if (separatorIdx != -1) {
            url  = args.substring(0, separatorIdx).trim();
            note = args.substring(separatorIdx + 3).trim();
        } else {
            url = args.trim();
        }

        // Basic URL validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "⚠️ Please provide a valid URL starting with http:// or https://";
        }

        try {
            // Fetch metadata from the URL using Jsoup
            String title       = url;
            String description = null;
            String contentType = guessContentType(url);

            try {
                // SEC-3: Block SSRF — reject private/loopback IPs before connecting
                assertNotPrivateUrl(url);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (ReelVaultBot/1.0)")
                        .timeout(10_000)
                        .maxBodySize(500_000)  // MED-2: 500KB max body to prevent memory DoS
                        .get();

                title = doc.title();
                if (title == null || title.isBlank()) title = url;

                Element metaDesc = doc.selectFirst("meta[name=description]");
                if (metaDesc == null) metaDesc = doc.selectFirst("meta[property=og:description]");
                if (metaDesc != null) description = metaDesc.attr("content");

            } catch (SecurityException se) {
                // SSRF attempt blocked
                return "\u26a0\uFE0F " + se.getMessage();
            } catch (Exception e) {
                log.warn("Jsoup could not fetch {}: {}", url, e.getMessage());
                // Proceed with URL as title
            }

            KnowledgeItem saved = itemService.quickSave(
                    url, null, title, description, contentType, new String[0], note
            );

            return String.format(
                    "✅ *Saved!*\n\n📌 *%s*\n🔗 %s%s\n🏷️ Type: `%s`\n🆔 ID: `%s`",
                    escapeMarkdown(title),
                    url,
                    note != null ? "\n📝 Note: " + escapeMarkdown(note) : "",
                    contentType,
                    saved.getId().toString().substring(0, 8) + "..."
            );

        } catch (Exception e) {
            log.error("Error saving URL {}: {}", url, e.getMessage(), e);
            return "❌ Failed to save: " + e.getMessage();
        }
    }

    /**
     * /search [query] — Semantic search, returns top 5 matches.
     */
    private String handleSearch(String query) {
        if (query.isBlank()) {
            return "⚠️ Usage: `/search AI agent that can browse the web`";
        }

        try {
            List<SearchResultDto> results = searchService.search(query, 5);

            if (results.isEmpty()) {
                return "🔍 No matching results found for: *" + escapeMarkdown(query) + "*\n\n_Try saving more items first._";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🔍 *Search: ").append(escapeMarkdown(query)).append("*\n");
            sb.append("─────────────────────\n\n");

            for (int i = 0; i < results.size(); i++) {
                SearchResultDto r = results.get(i);
                double score = r.getSimilarity();
                String scoreBar  = buildScoreBar(score);
                String scoreText = String.format("%.0f%%", score * 100);

                sb.append(i + 1).append(". *").append(escapeMarkdown(r.getTitle())).append("*\n");
                sb.append("   ").append(scoreBar).append(" ").append(scoreText).append("\n");
                if (r.getOriginalUrl() != null) {
                    sb.append("   🔗 ").append(r.getOriginalUrl()).append("\n");
                }
                if (r.getContentType() != null) {
                    sb.append("   🏷️ `").append(r.getContentType()).append("`\n");
                }
                if (r.getTags() != null && !r.getTags().isEmpty()) {
                    sb.append("   🔖 ").append(String.join(" · ", r.getTags())).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Search error for query '{}': {}", query, e.getMessage(), e);
            return "❌ Search failed: " + e.getMessage();
        }
    }

    /**
     * /recent — Shows last 5 saved items.
     */
    private String handleRecent() {
        try {
            List<KnowledgeItem> items = itemService.findRecent();
            if (items.isEmpty()) {
                return "📭 No items saved yet. Use /save to add your first link!";
            }

            // Show only top 5 in Telegram
            List<KnowledgeItem> display = items.subList(0, Math.min(5, items.size()));

            StringBuilder sb = new StringBuilder("🕐 *Recently Saved*\n─────────────────────\n\n");
            for (int i = 0; i < display.size(); i++) {
                KnowledgeItem item = display.get(i);
                sb.append(i + 1).append(". *").append(escapeMarkdown(item.getTitle())).append("*\n");
                if (item.getOriginalUrl() != null) {
                    sb.append("   🔗 ").append(item.getOriginalUrl()).append("\n");
                }
                if (item.getCreatedAt() != null) {
                    sb.append("   📅 ").append(item.getCreatedAt().format(DISPLAY_FMT)).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("Error fetching recent items: {}", e.getMessage(), e);
            return "❌ Could not fetch recent items.";
        }
    }

    /**
     * /digest — Items saved in the last 24 hours.
     */
    private String handleDigest() {
        try {
            List<KnowledgeItem> items = itemService.findDigest(24);

            if (items.isEmpty()) {
                return "📭 No items saved in the last 24 hours.\n\nUse /save to add something!";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 *Daily Digest — Last 24 Hours*\n");
            sb.append("(").append(items.size()).append(" item").append(items.size() == 1 ? "" : "s").append(")\n");
            sb.append("─────────────────────\n\n");

            for (int i = 0; i < items.size(); i++) {
                KnowledgeItem item = items.get(i);
                sb.append(i + 1).append(". *").append(escapeMarkdown(item.getTitle())).append("*\n");
                if (item.getContentType() != null) {
                    sb.append("   🏷️ `").append(item.getContentType()).append("`");
                }
                if (item.getTags() != null && item.getTags().length > 0) {
                    sb.append("  🔖 ").append(String.join(", ", item.getTags()));
                }
                sb.append("\n");
                if (item.getOriginalUrl() != null) {
                    sb.append("   🔗 ").append(item.getOriginalUrl()).append("\n");
                }
                if (item.getCreatedAt() != null) {
                    sb.append("   📅 ").append(item.getCreatedAt().format(DISPLAY_FMT)).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Error fetching digest: {}", e.getMessage(), e);
            return "❌ Could not fetch digest.";
        }
    }

    /**
     * /list [tag] — Filter items by tag.
     */
    private String handleList(String tag) {
        if (tag.isBlank()) {
            return "⚠️ Usage: `/list python` or `/list machine-learning`";
        }

        try {
            List<KnowledgeItem> items = itemService.findAll(null, tag.trim(), null, null);

            if (items.isEmpty()) {
                return "📭 No items found with tag: *" + escapeMarkdown(tag) + "*";
            }

            // Show max 10 in Telegram
            List<KnowledgeItem> display = items.subList(0, Math.min(10, items.size()));

            StringBuilder sb = new StringBuilder();
            sb.append("🔖 *Items tagged:* `").append(tag).append("`\n");
            sb.append("(").append(items.size()).append(" total)\n");
            sb.append("─────────────────────\n\n");

            for (int i = 0; i < display.size(); i++) {
                KnowledgeItem item = display.get(i);
                sb.append(i + 1).append(". *").append(escapeMarkdown(item.getTitle())).append("*\n");
                if (item.getOriginalUrl() != null) {
                    sb.append("   🔗 ").append(item.getOriginalUrl()).append("\n");
                }
                sb.append("\n");
            }

            if (items.size() > 10) {
                sb.append("_... and ").append(items.size() - 10).append(" more. Use the web UI to see all._");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Error listing items by tag '{}': {}", tag, e.getMessage(), e);
            return "❌ Could not list items for tag: " + tag;
        }
    }

    /**
     * /help — Show all available commands.
     */
    private String helpMessage() {
        return """
                📚 *ReelVault Bot Commands*
                ─────────────────────────
                
                💾 *Saving*
                `/save [url]` — Auto-fetch title and save
                `/save [url] - [note]` — Save with personal note
                
                🔍 *Searching*
                `/search [query]` — Semantic search (natural language)
                `/list [tag]` — Filter by tag
                
                📋 *Browsing*
                `/recent` — Last 5 saved items
                `/digest` — Items saved in last 24 hours
                
                ℹ️ *Other*
                `/help` — Show this message
                
                _Open the web app for full features: filtering, editing, and more._
                """;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void sendText(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.disableWebPagePreview();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Extract the command part from the message text.
     * e.g. "/save https://example.com" → "/save"
     * Also strips @BotUsername suffix from commands.
     */
    private String extractCommand(String text) {
        String[] parts = text.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        // Strip @username if present (e.g. /save@ReelVaultBot)
        int atIdx = cmd.indexOf('@');
        if (atIdx != -1) cmd = cmd.substring(0, atIdx);
        return cmd;
    }

    /**
     * Extract the arguments part from the message text (everything after the command).
     */
    private String extractArgs(String text) {
        int spaceIdx = text.indexOf(' ');
        if (spaceIdx == -1) return "";
        return text.substring(spaceIdx).trim();
    }

    /**
     * Guess the content type from the URL domain/path.
     */
    private String guessContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("youtube.com") || lower.contains("youtu.be")
                || lower.contains("vimeo.com") || lower.contains("instagram.com")) {
            return "video";
        }
        if (lower.contains("github.com") || lower.contains("gitlab.com")) {
            return "tool";
        }
        if (lower.contains("arxiv.org") || lower.contains("paper")) {
            return "paper";
        }
        if (lower.contains("coursera.org") || lower.contains("udemy.com")
                || lower.contains("edx.org") || lower.contains("pluralsight.com")) {
            return "course";
        }
        return "article";
    }

    /**
     * Build a simple ASCII similarity score bar for Telegram messages.
     * e.g. 0.85 → "█████████░"
     */
    private String buildScoreBar(double score) {
        int filled  = (int) Math.round(score * 10);
        int empty   = 10 - filled;
        return "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, empty));
    }

    /**
     * Escape Markdown special characters to prevent formatting issues.
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }

    /**
     * SEC-3: SSRF protection — reject URLs resolving to private/loopback IP ranges.
     * Blocks: localhost, 127.x.x.x, 10.x.x.x, 172.16-31.x.x, 192.168.x.x,
     * 169.254.x.x (AWS metadata), ::1 (IPv6 loopback).
     *
     * @throws SecurityException if the URL resolves to a private address
     */
    private void assertNotPrivateUrl(String url) throws SecurityException {
        try {
            String host = URI.create(url).getHost();
            if (host == null) throw new SecurityException("Invalid URL: no host");

            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                throw new SecurityException("URL resolves to a private/reserved IP address and is blocked for security.");
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Could not resolve URL host: " + e.getMessage());
        }
    }
}
