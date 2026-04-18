# ReelVault — V1 Codebase Reference Document

> **Version:** V1 — Post Security & Bug Audit (2026-04-15)
> **Purpose:** Complete codebase map for efficient future changes. Read this before editing any file.
> **Rule:** After every session that modifies code, create a new `V<n>_Document.md` to reflect the current state.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Directory Structure](#2-directory-structure)
3. [Tech Stack & Dependencies](#3-tech-stack--dependencies)
4. [Environment Variables (Complete List)](#4-environment-variables-complete-list)
5. [Database — sql/setup.sql](#5-database--sqlsetupsql)
6. [Backend — Java Spring Boot](#6-backend--java-spring-boot)
   - 6.1 [Entry Point](#61-entry-point)
   - 6.2 [Configuration — application.yml](#62-configuration--applicationyml)
   - 6.3 [pom.xml Dependencies](#63-pomxml-dependencies)
   - 6.4 [Model — KnowledgeItem.java](#64-model--knowledgeitemjava)
   - 6.5 [DTOs](#65-dtos)
   - 6.6 [Repository — KnowledgeItemRepository.java](#66-repository--knowledgeitemrepositoryjava)
   - 6.7 [Services](#67-services)
   - 6.8 [Controllers](#68-controllers)
   - 6.9 [Config — CorsConfig.java](#69-config--corsconfigjava)
   - 6.10 [Utility — VectorUtils.java](#610-utility--vectorutilsjava)
7. [Frontend — Vanilla JS SPA](#7-frontend--vanilla-js-spa)
   - 7.1 [index.html](#71-indexhtml)
   - 7.2 [js/env.js](#72-jsenvjs)
   - 7.3 [js/api.js](#73-jsapijs)
   - 7.4 [js/app.js](#74-jsappjs)
   - 7.5 [js/home.js](#75-jshomejs)
   - 7.6 [js/browse.js](#76-jsbrowsejs)
   - 7.7 [js/add.js](#77-jsaddjs)
   - 7.8 [css/style.css](#78-cssstylecss)
8. [API Contract (All Endpoints)](#8-api-contract-all-endpoints)
9. [Data Flow Diagrams](#9-data-flow-diagrams)
10. [Security Hardening (Post-Audit)](#10-security-hardening-post-audit)
11. [Known Gotchas & Design Decisions](#11-known-gotchas--design-decisions)
12. [Deployment Guide](#12-deployment-guide)
13. [Change Log](#13-change-log)

---

## 1. Project Overview

**ReelVault** is a personal semantic knowledge base. The user saves links, reels, notes, and resources discovered on social media. Later, they search them using natural language (semantic search powered by pgvector + sentence embeddings).

### Core Features
| Feature | Implementation |
|---|---|
| Save content (URL, title, description, tags, type, notes) | POST /api/items |
| Browse & filter saved items | GET /api/items + filters |
| Natural language semantic search | POST /api/search via pgvector cosine distance |
| Telegram bot (save, search, recent, digest) | TelegramLongPollingBot |
| Web SPA (dark glassmorphism UI) | Vanilla JS ES Modules, hash router |

---

## 2. Directory Structure

```
Personal_Knowledge_Base_Web_Application/
│
├── sql/
│   └── setup.sql                    ← PostgreSQL schema + HNSW index (run once in Supabase SQL Editor)
│
├── backend/
│   ├── pom.xml                      ← Maven build (Java 21, Spring Boot 3.2)
│   └── src/main/
│       ├── resources/
│       │   └── application.yml      ← All Spring config + env var bindings
│       └── java/com/reelvault/
│           ├── ReelVaultApplication.java         ← @SpringBootApplication entry point
│           ├── config/
│           │   └── CorsConfig.java               ← CORS (restricts to frontend domain)
│           ├── controller/
│           │   ├── ItemController.java            ← REST: /api/items
│           │   └── SearchController.java          ← REST: /api/search
│           ├── dto/
│           │   ├── ItemRequest.java               ← POST/PUT request body
│           │   ├── SearchRequest.java             ← POST /api/search request body
│           │   └── SearchResultDto.java           ← Search response with similarity score
│           ├── model/
│           │   └── KnowledgeItem.java             ← JPA entity ↔ knowledge_items table
│           ├── repository/
│           │   └── KnowledgeItemRepository.java   ← pgvector native SQL queries
│           ├── service/
│           │   ├── EmbeddingService.java          ← HuggingFace API → float[384]
│           │   ├── ItemService.java               ← CRUD business logic
│           │   ├── SearchService.java             ← Semantic search + row mapping
│           │   └── TelegramBotService.java        ← Telegram bot (LongPolling)
│           └── util/
│               └── VectorUtils.java               ← float[] ↔ "[f1,f2,...,f384]" string
│
├── frontend/
│   ├── index.html                   ← Single HTML file (all pages as divs)
│   ├── css/
│   │   └── style.css                ← Full dark glassmorphism design system
│   └── js/
│       ├── env.js                   ← window.REELVAULT_API_URL (change before deploy)
│       ├── api.js                   ← All fetch calls to backend (ES module)
│       ├── app.js                   ← Hash router + shared utils (escapeHtml, modal, toast)
│       ├── home.js                  ← Semantic search UI + card builder
│       ├── browse.js                ← Browse + filter + delete
│       └── add.js                   ← Save form handler
│
├── V1_Document.md                   ← THIS FILE — full codebase reference
└── README.md                        ← Deployment guide (Railway + Vercel + Supabase)
```

---

## 3. Tech Stack & Dependencies

### Backend
| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.2.0 |
| Build | Maven | (via pom.xml) |
| Database driver | PostgreSQL JDBC | Managed by Boot |
| ORM | Spring Data JPA / Hibernate | Managed by Boot |
| Array types | hypersistence-utils-hibernate-63 | 3.7.0 |
| HTTP client (outbound) | Java 11 HttpClient | Built-in |
| JSON | Jackson (databind + jsr310) | Managed by Boot |
| Validation | jakarta.validation | Managed by Boot |
| Telegram bot | telegrambots + telegrambots-meta | 6.9.7.1 |
| HTML scraping | Jsoup | 1.17.1 |

### Database
| Component | Detail |
|---|---|
| Host | Supabase PostgreSQL |
| Extension | pgvector (vector similarity) |
| Embedding dimension | 384 (sentence-transformers/all-MiniLM-L6-v2) |
| Index type | HNSW (cosine ops, m=16, ef_construction=64) |

### Frontend
| Component | Detail |
|---|---|
| Language | Vanilla JavaScript (ES Modules, no bundler) |
| Design | Glassmorphism dark theme, CSS custom properties |
| Router | Hash-based (#home, #browse, #add) |
| Module loading | `<script type="module">` — only app.js loaded in HTML |
| Font | Inter (Google Fonts) |

---

## 4. Environment Variables (Complete List)

Set all of these in Railway for the backend. None have defaults except noted.

| Variable | Where Used | Example | Default |
|---|---|---|---|
| `SUPABASE_DB_URL` | datasource.url | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` | **required** |
| `SUPABASE_DB_USER` | datasource.username | `postgres` | **required** |
| `SUPABASE_DB_PASSWORD` | datasource.password | `your_db_password` | **required** |
| `HUGGINGFACE_API_KEY` | app.huggingface.api-key | `hf_xxxxxxxxxxxxxx` | **required** |
| `TELEGRAM_BOT_TOKEN` | app.telegram.bot-token | `123456:ABCdef...` | **required** |
| `TELEGRAM_BOT_USERNAME` | app.telegram.bot-username | `ReelVaultBot` | **required** |
| `TELEGRAM_ALLOWED_CHAT_ID` | app.telegram.allowed-chat-id | `987654321` (get from @userinfobot) | `""` (open bot — set this!) |
| `CORS_ALLOWED_ORIGIN` | app.cors.allowed-origin | `https://reelvault.vercel.app` | `http://localhost:5500` |
| `PORT` | server.port | `8080` | `8080` |

**Frontend env var** (set in `frontend/js/env.js` before deploying to Vercel):
```js
window.REELVAULT_API_URL = 'https://your-railway-app.up.railway.app';
```

---

## 5. Database — sql/setup.sql

**Run once** in Supabase SQL Editor before first deployment.

### Table: `knowledge_items`
| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, `gen_random_uuid()` |
| `title` | TEXT | NOT NULL |
| `description` | TEXT | nullable |
| `original_url` | TEXT | nullable (the actual resource URL) |
| `source_url` | TEXT | nullable (where you found it, e.g. Instagram) |
| `content_type` | TEXT | CHECK: `tool\|article\|video\|paper\|course\|note` |
| `tags` | TEXT[] | PostgreSQL array, nullable |
| `personal_notes` | TEXT | nullable |
| `embedding` | vector(384) | pgvector; null if embedding generation failed |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW() |
| `updated_at` | TIMESTAMPTZ | DEFAULT NOW(), auto-updated by trigger |

### Indexes
| Index | Type | Purpose |
|---|---|---|
| `knowledge_items_embedding_idx` | HNSW (cosine ops, m=16, ef_construction=64) | Fast semantic search |
| `knowledge_items_content_type_idx` | B-tree | Filter by content_type |
| `knowledge_items_created_at_idx` | B-tree DESC | Recent items + date range |
| `knowledge_items_tags_idx` | GIN | Tag-based filtering (ANY operator) |

### Auto-update trigger
`set_updated_at` trigger calls `update_updated_at_column()` BEFORE UPDATE on every row.

> **Upgrading from IVFFlat?** Run: `DROP INDEX IF EXISTS knowledge_items_embedding_idx;` then re-run Step 3 of setup.sql to create the HNSW index.

---

## 6. Backend — Java Spring Boot

### 6.1 Entry Point

**File:** `backend/src/main/java/com/reelvault/ReelVaultApplication.java`

```java
@SpringBootApplication
public class ReelVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReelVaultApplication.class, args);
    }
}
```
No custom config here — everything is in `application.yml`.

---

### 6.2 Configuration — application.yml

**File:** `backend/src/main/resources/application.yml`
**Total lines:** 120

Key sections:
- **Lines 7–26:** DataSource (Hikari pool: max 5 connections, 30s timeout)
- **Lines 31–46:** JPA — `ddl-auto: validate` (NEVER use create/create-drop in prod)
- **Lines 56–66:** Server port + **error responses set to `never`** (security)
- **Lines 71–88:** Custom app properties (HuggingFace URL, Telegram, CORS, allowed chat ID)
- **Lines 93–99:** Logging (INFO for com.reelvault, WARN for Hibernate SQL)
- **Lines 101–119:** DEV profile — activate with `--spring.profiles.active=dev` to enable show-sql and verbose errors

**Critical security settings:**
```yaml
server:
  error:
    include-message: never        # Never expose internal messages in prod
    include-binding-errors: never
    include-stacktrace: never
    include-exception: false
```

**HuggingFace API URL** (hardcoded in yml, not env var):
```
https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2
```

---

### 6.3 pom.xml Dependencies

**File:** `backend/pom.xml` | Java 21 | Spring Boot 3.2.0

| Artifact | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA/Hibernate |
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Pattern` |
| `postgresql` (runtime) | JDBC driver |
| `hypersistence-utils-hibernate-63` v3.7.0 | `StringArrayType` for TEXT[] |
| `telegrambots` v6.9.7.1 | LongPolling bot |
| `telegrambots-meta` v6.9.7.1 | Telegram API types |
| `jsoup` v1.17.1 | URL metadata scraping |
| `jackson-databind` | JSON serialization |
| `jackson-datatype-jsr310` | Java 8 time types in JSON |
| `spring-boot-starter-test` (test) | JUnit 5 |

**Build output:** `target/reelvault.jar` (Railway start command: `java -jar target/reelvault.jar`)

---

### 6.4 Model — KnowledgeItem.java

**File:** `backend/src/main/java/com/reelvault/model/KnowledgeItem.java`
**Package:** `com.reelvault.model`

| Field | Java Type | Column | Notes |
|---|---|---|---|
| `id` | UUID | `uuid` PK | `@GeneratedValue(UUID)`, no `setId()` |
| `title` | String | TEXT NOT NULL | |
| `description` | String | TEXT | nullable |
| `originalUrl` | String | `original_url` TEXT | nullable |
| `sourceUrl` | String | `source_url` TEXT | nullable |
| `contentType` | String | `content_type` | nullable, SQL CHECK constraint |
| `tags` | String[] | `tags` text[] | `@Type(StringArrayType.class)` |
| `personalNotes` | String | `personal_notes` TEXT | nullable |
| `embedding` | String | `vector(384)` | Stored as `"[f1,f2,...,f384]"`, PG auto-casts |
| `createdAt` | OffsetDateTime | `created_at` TIMESTAMPTZ | Set in `@PrePersist` |
| `updatedAt` | OffsetDateTime | `updated_at` TIMESTAMPTZ | Set in `@PrePersist` + `@PreUpdate` |

**Important:** No `setId()` — UUID is DB-generated. Never call `item.setId(...)`.

**Lifecycle hooks:**
- `@PrePersist onCreate()` — sets both `createdAt` and `updatedAt` to `OffsetDateTime.now()`
- `@PreUpdate onUpdate()` — sets `updatedAt` to `OffsetDateTime.now()`

---

### 6.5 DTOs

#### ItemRequest.java
**File:** `backend/src/main/java/com/reelvault/dto/ItemRequest.java`
**Used by:** POST /api/items, PUT /api/items/{id}

| Field | Validation | JSON key |
|---|---|---|
| `title` | `@NotBlank`, `@Size(max=500)` | `title` |
| `description` | `@Size(max=5000)` | `description` |
| `originalUrl` | `@Pattern(^(https?://.*)?$)` | `original_url` |
| `sourceUrl` | `@Pattern(^(https?://.*)?$)` | `source_url` |
| `contentType` | `@Pattern(^(tool\|article\|video\|paper\|course\|note)$)` | `content_type` |
| `tags` | none | `tags` (List\<String\>) |
| `personalNotes` | none | `personal_notes` |

> **URL validation:** Both URL fields allow null/empty but reject `javascript:`, `data:`, `ftp:`, and other non-HTTP schemes.

#### SearchRequest.java
**File:** `backend/src/main/java/com/reelvault/dto/SearchRequest.java`
**Used by:** POST /api/search

| Field | Validation | Default |
|---|---|---|
| `query` | `@NotBlank`, `@Size(max=500)` | required |
| `limit` | `@Min(1)`, `@Max(50)` | 10 |

#### SearchResultDto.java
**File:** `backend/src/main/java/com/reelvault/dto/SearchResultDto.java`
**Returned by:** POST /api/search

All fields same as KnowledgeItem PLUS:
- `similarity` (double) — cosine similarity score 0.0–1.0 (higher = more relevant)
- `id` is String (not UUID) — from native query result
- Timestamps are String (ISO format from `.toString()` on JDBC object)
- `setTagsFromArray(String[])` — helper to convert JDBC array to `List<String>`

---

### 6.6 Repository — KnowledgeItemRepository.java

**File:** `backend/src/main/java/com/reelvault/repository/KnowledgeItemRepository.java`
**Extends:** `JpaRepository<KnowledgeItem, UUID>`

#### Methods

| Method | Type | Purpose |
|---|---|---|
| `findSimilarItems(String queryVector, int limit)` | Native SQL → `List<Object[]>` | pgvector cosine similarity search |
| `findWithFilters(contentType, tag, dateFrom, dateTo)` | Native SQL → `List<KnowledgeItem>` | Filtered browse (all params nullable) |
| `findTop10ByOrderByCreatedAtDesc()` | Derived | Last 10 items |
| `findRecentByHours(int hours)` | Native SQL → `List<KnowledgeItem>` | Items from last N hours (Telegram /digest) |
| `findById(UUID)` | Inherited | Single item |
| `save(KnowledgeItem)` | Inherited | Insert + Update |
| `existsById(UUID)` | Inherited | Check existence before delete |
| `deleteById(UUID)` | Inherited | Delete |

#### Key SQL Patterns

**Semantic search query** (returns 11-column Object[]):
```sql
SELECT id, title, description, original_url, source_url, content_type,
       tags, personal_notes, created_at, updated_at,
       1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
FROM knowledge_items
WHERE embedding IS NOT NULL
ORDER BY embedding <=> CAST(:queryVector AS vector)
LIMIT :limit
```

**Filter query:**
```sql
WHERE (:contentType IS NULL OR content_type = :contentType)
  AND (:tag IS NULL OR :tag = ANY(tags))
  AND (:dateFrom IS NULL OR created_at >= CAST(:dateFrom AS timestamptz))
  AND (:dateTo IS NULL OR created_at <= CAST(:dateTo AS timestamptz))
ORDER BY created_at DESC
```

**Interval query fix** (BUG-7 — use explicit CAST):
```sql
WHERE created_at >= NOW() - CAST(:hours AS int) * INTERVAL '1 hour'
```

---

### 6.7 Services

#### EmbeddingService.java
**File:** `backend/src/main/java/com/reelvault/service/EmbeddingService.java`
**Lines:** 220

**Key methods:**
| Method | Signature | Purpose |
|---|---|---|
| `generateEmbedding` | `(String text) → float[]` | Calls HuggingFace API, returns 384-dim vector |
| `buildEmbeddingText` | `(title, description, tags[], notes) → String` | Builds combined text for embedding |
| `parseEmbedding` | private `(String responseBody) → float[]` | Handles 1D, 2D, 3D response formats |
| `toFloatArray` | private `(List<?>) → float[]` | Convert Number list to float[] |
| `meanPool` | private `(List<List<Object>>) → float[]` | Mean pooling for token-level embeddings |

**HuggingFace call flow:**
1. Truncate text to 2000 chars (MED-1 fix)
2. POST JSON `{"inputs": text}` with `Authorization: Bearer {apiKey}`
3. If 503 (model loading): sleep 20s, retry once
4. If non-200: throw RuntimeException with truncated body (SEC-2 fix)
5. Parse response via `parseEmbedding()`

**Response format handling:**
- 1D `[f1, f2, ...]` → use directly
- 2D `[[f1, f2, ...]]` single row → use row[0]
- 2D multiple rows → mean pool
- 3D token-level → mean pool over tokens

**Interrupt handling:** `IOException` and `InterruptedException` caught in separate blocks. `InterruptedException` restores interrupt flag before rethrowing (BUG-2 fix).

---

#### ItemService.java
**File:** `backend/src/main/java/com/reelvault/service/ItemService.java`
**Lines:** 242 | `@Service @Transactional`

**Key methods:**
| Method | Signature | Notes |
|---|---|---|
| `save` | `(ItemRequest) → KnowledgeItem` | Auto-generates embedding; saves without embedding if API fails |
| `quickSave` | `(url, sourceUrl, title, desc, type, tags[], notes) → KnowledgeItem` | Used by Telegram bot |
| `findAll` | `(contentType, tag, dateFrom, dateTo) → List<KnowledgeItem>` | All params nullable |
| `findById` | `(@NonNull UUID) → Optional<KnowledgeItem>` | `@Transactional(readOnly=true)` |
| `findRecent` | `() → List<KnowledgeItem>` | Top 10 by created_at DESC |
| `findDigest` | `(int hours) → List<KnowledgeItem>` | Items from last N hours |
| `update` | `(@NonNull UUID, ItemRequest) → KnowledgeItem` | Re-embeds only if text changed (BUG-1: uses Objects.equals, not String.valueOf) |
| `delete` | `(@NonNull UUID) → void` | Checks exists first, then deleteById |

**Embedding re-generation logic (update):**
```java
boolean textChanged =
    !Objects.equals(request.getTitle(), item.getTitle()) ||
    !Objects.equals(request.getDescription(), item.getDescription()) ||
    !Objects.equals(request.getPersonalNotes(), item.getPersonalNotes());
```
Tags changes do NOT trigger re-embedding (optimization — acceptable tradeoff).

---

#### SearchService.java
**File:** `backend/src/main/java/com/reelvault/service/SearchService.java`
**Lines:** 147 | `@Service @Transactional(readOnly=true)`

**Single public method:**
```java
public List<SearchResultDto> search(String query, int limit)
```

**Column index constants** (critical — must match SELECT order in findSimilarItems):
```
COL_ID=0, COL_TITLE=1, COL_DESCRIPTION=2, COL_ORIGINAL_URL=3, COL_SOURCE_URL=4,
COL_CONTENT_TYPE=5, COL_TAGS=6, COL_PERSONAL_NOTES=7,
COL_CREATED_AT=8, COL_UPDATED_AT=9, COL_SIMILARITY=10
```

**Tags handling** (BUG-8 fix — handles both JDBC return types):
```java
if (row[COL_TAGS] instanceof java.sql.Array sqlArray) {
    dto.setTagsFromArray((String[]) sqlArray.getArray());
} else {
    // Fallback: parse "{tag1,tag2}" string format
    String raw = row[COL_TAGS].toString().trim();
    // strip { }, split by comma
}
```

---

#### TelegramBotService.java
**File:** `backend/src/main/java/com/reelvault/service/TelegramBotService.java`
**Lines:** ~490 | `@Service`, extends `TelegramLongPollingBot`

**Bot commands:**
| Command | Handler | Description |
|---|---|---|
| `/start` | inline | Welcome message |
| `/save <url> [#tag1 #tag2] [note text]` | `handleSave(args)` | Save URL with auto-metadata fetch |
| `/search <query>` | `handleSearch(args)` | Semantic search, returns top 5 |
| `/recent` | `handleRecent()` | Last 10 saved items |
| `/digest` | `handleDigest()` | Items saved in last 24h |
| `/list #tag` or `/list type:video` | `handleList(args)` | Filter items |
| `/help` | `helpMessage()` | Show all commands |

**Security features added:**
- **SEC-4:** `allowedChatId` check — rejects any chatId not matching `TELEGRAM_ALLOWED_CHAT_ID`
- **SEC-3:** `assertNotPrivateUrl(url)` — blocks SSRF via loopback/private/link-local IPs using `InetAddress` resolution before Jsoup fetch

**Jsoup metadata fetch:**
```java
Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (ReelVaultBot/1.0)")
    .timeout(10_000)
    .maxBodySize(500_000)   // 500KB limit (MED-2)
    .get();
```
Falls back to URL as title if fetch fails.

**`handleSave` argument parsing:**
- First token = URL
- Tokens starting with `#` = tags
- `type:tool` style = content type
- Remaining text = personal notes

---

### 6.8 Controllers

#### ItemController.java
**File:** `backend/src/main/java/com/reelvault/controller/ItemController.java`
**Base path:** `/api/items`

| Endpoint | Method | Handler | Response |
|---|---|---|---|
| `/api/items` | POST | `createItem(@Valid ItemRequest)` | 201 + KnowledgeItem |
| `/api/items` | GET | `listItems(contentType, tag, dateFrom, dateTo)` | 200 + List |
| `/api/items/recent` | GET | `getRecentItems()` | 200 + List (max 10) |
| `/api/items/{id}` | GET | `getItem(@NonNull UUID)` | 200 or 404 |
| `/api/items/{id}` | PUT | `updateItem(@NonNull UUID, @Valid ItemRequest)` | 200 or 404 |
| `/api/items/{id}` | DELETE | `deleteItem(@NonNull UUID)` | 204 or 404 |

All UUID `@PathVariable` params annotated `@NonNull` to satisfy null-type-safety linting.

#### SearchController.java
**File:** `backend/src/main/java/com/reelvault/controller/SearchController.java`
**Base path:** `/api/search`

| Endpoint | Method | Handler | Response |
|---|---|---|---|
| `/api/search` | POST | `search(@Valid SearchRequest)` | 200 + List\<SearchResultDto\> |

---

### 6.9 Config — CorsConfig.java

**File:** `backend/src/main/java/com/reelvault/config/CorsConfig.java`

```java
registry.addMapping("/api/**")
    .allowedOrigins(allowedOrigin)    // from CORS_ALLOWED_ORIGIN env var
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("Content-Type", "Accept", "Authorization", "X-Requested-With")
    .exposedHeaders("Content-Type")
    .allowCredentials(false)
    .maxAge(3600);
```

> **Note:** `allowedOrigin` comes from `${app.cors.allowed-origin}` which defaults to `http://localhost:5500`.

---

### 6.10 Utility — VectorUtils.java

**File:** `backend/src/main/java/com/reelvault/util/VectorUtils.java`

| Method | Signature | Purpose |
|---|---|---|
| `toVectorString` | `(float[]) → String` | Converts `[0.1, 0.2, ...]` array to `"[0.1,0.2,...]"` string for pgvector |
| `fromVectorString` | `(String) → float[]` | Parses `"[f1,f2,...]"` back to float[] (used for testing/validation) |

**Error handling** (MED-3 fix): `Float.parseFloat()` is wrapped in try-catch that throws `IllegalArgumentException` with index context on failure:
```java
throw new IllegalArgumentException(
    "Corrupt vector string at index " + i + ": could not parse '" + parts[i].trim() + "'", e
);
```

---

## 7. Frontend — Vanilla JS SPA

### 7.1 index.html

**File:** `frontend/index.html` | ~370 lines

**Page sections (all in one HTML file):**
- `#page-home` — Search page (default)
- `#page-browse` — Browse + filter
- `#page-add` — Save new item form

**Key element IDs:**
| ID | Element | Purpose |
|---|---|---|
| `page-home` | div | Home page container |
| `page-browse` | div | Browse page container |
| `page-add` | div | Add item page container |
| `nav-home`, `nav-browse`, `nav-add` | `<a>` | Nav links |
| `search-input` | input | Main search box |
| `search-btn` | button | Search button |
| `search-chips` | div | Example query chips |
| `search-loader` | div | Loading spinner |
| `search-empty` | div | No results state |
| `results-header` | div | "N results for '...'" |
| `results-count` | span | Result count |
| `results-query` | span | Query echo |
| `results-grid` | div | Search result cards |
| `browse-grid` | div | Browse cards |
| `browse-loader` | div | Browse loading spinner |
| `browse-empty` | div | Empty browse state |
| `browse-stats` | span | "N items (2 filters active)" |
| `filter-type` | select | Content type filter |
| `filter-tag` | input | Tag filter |
| `filter-date-from` | input[date] | Date from filter |
| `filter-date-to` | input[date] | Date to filter |
| `btn-apply-filter` | button | Apply filters |
| `btn-clear-filter` | button | Clear filters |
| `save-form` | form | Add item form |
| `form-title` | input | Title field |
| `form-description` | textarea | Description |
| `form-original-url` | input | Original URL |
| `form-source-url` | input | Source URL |
| `form-type` | select | Content type |
| `form-tags` | input | Tags (comma-separated) |
| `form-notes` | textarea | Personal notes |
| `tag-preview` | div | Live tag badges preview |
| `btn-save` | button | Submit form |
| `form-success` | div | Save success message |
| `form-success-text` | span | Success text |
| `form-error` | div | Save error message |
| `form-error-text` | span | Error text |
| `error-title` | span | Title field error |
| `modal-overlay` | div | Modal backdrop |
| `modal-content` | div | Modal inner content |
| `modal-close` | button | Close modal X |
| `toast-container` | div | Toast notifications |

**Script loading** (BUG-6 fix — only one script tag needed):
```html
<script type="module" src="js/app.js"></script>
```
`app.js` imports all other modules via ES `import`.

---

### 7.2 js/env.js

**File:** `frontend/js/env.js` | 10 lines

```js
window.REELVAULT_API_URL = '';
```

**Before deploying to Vercel, change to:**
```js
window.REELVAULT_API_URL = 'https://your-app.up.railway.app';
```

This file must be loaded via a regular `<script>` tag **before** `app.js`. It is already placed before the module script in `index.html`.

---

### 7.3 js/api.js

**File:** `frontend/js/api.js` | 160 lines | ES Module

All API calls return `{ data, error }` — never throw.

| Exported Function | HTTP | Path | Purpose |
|---|---|---|---|
| `saveItem(item)` | POST | `/api/items` | Save new item |
| `listItems(filters)` | GET | `/api/items?...` | Browse with filters |
| `getItem(id)` | GET | `/api/items/{id}` | Single item |
| `getRecentItems()` | GET | `/api/items/recent` | Last 10 items |
| `updateItem(id, item)` | PUT | `/api/items/{id}` | Update item |
| `deleteItem(id)` | DELETE | `/api/items/{id}` | Delete item |
| `searchItems(query, limit)` | POST | `/api/search` | Semantic search |

**`listItems` filter mapping:**
```js
filters.contentType → ?contentType=...
filters.tag         → ?tag=...
filters.dateFrom    → ?dateFrom=...T00:00:00Z
filters.dateTo      → ?dateTo=...T23:59:59Z
```

**Error handling:** On non-OK responses, returns `{ data: null, error: message }` where `message = data?.message || data?.error || 'HTTP {status}'`.

---

### 7.4 js/app.js

**File:** `frontend/js/app.js` | 289 lines | ES Module

**Exports:**
| Export | Signature | Purpose |
|---|---|---|
| `showToast` | `(message, type?, duration?) → void` | Show toast notification (success/error/info) |
| `openItemModal` | `(item) → void` | Open item detail modal |
| `escapeHtml` | `(str) → string` | XSS-safe HTML escaping |
| `typeIcon` | `(type) → string` | Emoji for content type |
| `formatDate` | `(dateStr) → string` | Relative date (Today/Yesterday/N days ago/date) |

**Router:**
- Hash-based: reads `window.location.hash.slice(1)` → calls `navigateTo(pageName)`
- Valid pages: `home`, `browse`, `add` (fallback: `home`)
- Page modules initialized **once** (tracked by `initialised` Set)
- Browse page gets `reelvault:browse:load` CustomEvent on every revisit to reload items

**navigateTo() logic** (BUG-3 fix):
1. Remove `active` class from ALL pages
2. Set `display: none` on ALL pages
3. Show target page (`display: ''`, add `active` class)
4. Update nav link classes
5. Update document.title
6. Initialize page module (once)

**Modal:**
- `openItemModal(item)` → renders `renderModalContent(item)` into `#modal-content`
- Close: click X button, click backdrop, or Escape key
- Escape listener uses `{ once: true }` (MED-5 fix — prevents listener accumulation)

**SEC-7 XSS protection in renderModalContent:**
```js
const VALID_TYPES = new Set(['tool', 'article', 'video', 'paper', 'course', 'note']);
const safeType = VALID_TYPES.has(item.content_type) ? item.content_type : 'unknown';
// safeType used in CSS class, item.content_type used in displayed text (escaped)
```

---

### 7.5 js/home.js

**File:** `frontend/js/home.js` | 232 lines | ES Module

**Exports:**
| Export | Purpose |
|---|---|
| `initHomePage()` | Wire search input, button, chips; focus input |
| `showGrid(items, showSimilarity?)` | Render cards into `#results-grid` |
| `buildCard(item, showSimilarity?, index?)` | Build single card `<article>` element |

**Search flow:**
1. User types + hits Enter (or clicks Search button, or clicks a chip)
2. `triggerSearch()` → shows loader, disables button
3. `searchItems(query, 10)` → `POST /api/search`
4. Renders results with similarity bar if `showSimilarity=true`
5. Shows "no results" empty state if 0 results

**buildCard() — important details:**
- Applies `animationDelay: ${index * 50}ms` (staggered entrance, capped at 500ms)
- SEC-7: `safeType` whitelist for CSS class injection
- Similarity bar rendered as `<div style="width:{pct}%">` (0–100%)
- Tags: shows max 5 tags, `+N` badge for additional
- URL: truncated to hostname + first 30 chars of path
- **Delete button** — has `addEventListener` for async delete with confirm dialog + card fade-out animation (BUG-4 fix)
- Open URL links: have `stopPropagation()` so they don't open the modal

---

### 7.6 js/browse.js

**File:** `frontend/js/browse.js` | 159 lines | ES Module

**Exports:** `initBrowsePage()`

**Imports:** `listItems`, `deleteItem` from api.js; `showToast`, `escapeHtml` from app.js; `buildCard` from home.js

**Key behavior:**
- `loadItems()` — fetches `GET /api/items` with current filter values, renders cards via `buildCard()`
- Filter state read from DOM elements on each load (stateless)
- `handleGridClick(e)` — event delegation on `#browse-grid` for delete buttons (safety net; primary handler is in `buildCard`)
- `confirmDelete(id, title, cardEl)` — shows `window.confirm()`, calls API, animates card out
- On revisit: `reelvault:browse:load` CustomEvent triggers `loadItems()` again
- `updateStats(count, filters)` — shows "N items (M filters active)" summary

**Note:** `renderGrid()` no longer attaches per-card delete handlers (LOW-4 fix) — `buildCard()` handles it.

---

### 7.7 js/add.js

**File:** `frontend/js/add.js` | 168 lines | ES Module

**Exports:** `initAddPage()`

**Form flow:**
1. `initAddPage()` — wire live tag preview (`input` event on `#form-tags`) and form `submit`
2. `handleSubmit(e)` → prevent default → `validateForm()` → `buildPayload()` → `saveItem(payload)`
3. On success: show `#form-success` with item ID, show toast, call `resetForm()`
4. On error: show `#form-error` and toast

**buildPayload()** — converts form values to API object:
```js
{
  title, description, original_url, source_url,
  content_type, tags: string[],  personal_notes
}
```
Fields with empty value are set to `undefined` (excluded from JSON).

**resetForm()** (BUG-5 fix):
```js
const tagPreview = document.getElementById('tag-preview');
if (tagPreview) tagPreview.innerHTML = '';  // null guard
```

**Submit button state:** Text changes to `"Saving… (generating AI embedding)"` while submitting.

---

### 7.8 css/style.css

**File:** `frontend/css/style.css` | ~600 lines

**Design system — key CSS custom properties:**
```css
--color-bg           /* Dark background #0a0a0f */
--color-surface      /* Card glass surface */
--color-surface-2    /* Elevated surface */
--color-border       /* Subtle borders */
--color-primary      /* Brand purple/blue */
--color-text         /* Main text */
--color-text-muted   /* Secondary text */
```

**Key component classes:**
| Class | Component |
|---|---|
| `.item-card` | Knowledge item card (glassmorphism, hover lift) |
| `.card__header` | Card title + type badge row |
| `.card__title` | Card heading |
| `.card__description` | Card body text (2-line clamp) |
| `.card__similarity` | Similarity bar container |
| `.similarity-bar` | Progress bar wrapper |
| `.similarity-bar__fill` | Colored fill (width set inline as %) |
| `.similarity-score` | Percentage text |
| `.card__url` | URL link with 🔗 icon |
| `.card__tags` | Tag badges row |
| `.tag-badge` | Individual tag pill |
| `.card__footer` | Date + action buttons row |
| `.card__action-btn` | Open / Delete button |
| `.card__action-btn--delete` | Delete button variant |
| `.type-badge` | Content type badge |
| `.type-badge--tool/article/video/...` | Color variants per type |
| `.toast` | Toast notification |
| `.toast--success/error/info` | Toast color variants |
| `#modal-overlay` | Full-screen modal backdrop |
| `.modal-detail` | Modal inner content |
| `.modal-detail__title` | Modal heading |
| `.modal-detail__field` | Label + value pair |
| `.modal-detail__label` | Gray label |
| `.modal-detail__value` | White value |
| `.modal-detail__actions` | Modal CTA buttons |
| `.btn` | Base button |
| `.btn--primary` | Filled accent button |
| `.btn--ghost` | Outline button |
| `.form-group` | Label + input wrapper |
| `.form-control` | Input/select/textarea |
| `.form-error` | Error text under field |
| `#form-success` | Green success banner |
| `#form-error` | Red error banner |
| `.chip` | Search example query chip |
| `.filter-bar` | Horizontal filter controls |

---

## 8. API Contract (All Endpoints)

### POST /api/items — Save Item
```
Request:  { title*, description, original_url, source_url, content_type, tags: [], personal_notes }
Response: 201 { id, title, description, original_url, source_url, content_type, tags, personal_notes,
                embedding, created_at, updated_at }
Errors:   400 { errors: [...] } on validation failure
```

### GET /api/items — List Items
```
Query params: contentType=tool&tag=ai&dateFrom=2024-01-01T00:00:00Z&dateTo=2024-12-31T23:59:59Z
Response: 200 [ ...KnowledgeItem ]
```

### GET /api/items/recent — Recent Items
```
Response: 200 [ ...KnowledgeItem ] (max 10, newest first)
```

### GET /api/items/{id} — Get Single Item
```
Response: 200 KnowledgeItem | 404
```

### PUT /api/items/{id} — Update Item
```
Request:  same as POST (title required)
Response: 200 KnowledgeItem | 404
Note:     Re-generates embedding if title/description/personal_notes changed
```

### DELETE /api/items/{id} — Delete Item
```
Response: 204 No Content | 404
```

### POST /api/search — Semantic Search
```
Request:  { query: "natural language query", limit: 10 }
Response: 200 [ { id, title, description, original_url, source_url, content_type,
                   tags, personal_notes, created_at, similarity: 0.0-1.0 } ]
Errors:   400 on blank query
```

---

## 9. Data Flow Diagrams

### Save Item Flow
```
User fills form (add.js)
  → buildPayload()
  → saveItem(payload) [api.js POST /api/items]
  → ItemController.createItem()
  → ItemService.save()
      → EmbeddingService.buildEmbeddingText(title, desc, tags, notes)
      → EmbeddingService.generateEmbedding(text)  ← truncate to 2000 chars first
          → HuggingFace API POST (with retry on 503)
          → parseEmbedding() → float[384]
      → VectorUtils.toVectorString(float[]) → "[f1,f2,...,f384]"
      → KnowledgeItem.setEmbedding(string)
  → KnowledgeItemRepository.save(item)
  → PostgreSQL: INSERT into knowledge_items (embedding casts string → vector)
  → Returns saved KnowledgeItem with generated UUID
```

### Semantic Search Flow
```
User types query (home.js)
  → searchItems(query, 10) [api.js POST /api/search]
  → SearchController.search()
  → SearchService.search(query, limit)
      → EmbeddingService.generateEmbedding(query) → float[384]
      → VectorUtils.toVectorString() → "[f1,f2,...,f384]"
      → KnowledgeItemRepository.findSimilarItems(queryVector, limit)
          → PostgreSQL: cosine distance <=> with HNSW index
          → Returns List<Object[]> with similarity column
      → SearchService.mapRow() for each row
          → Handles java.sql.Array or String[] for tags
          → Returns SearchResultDto with similarity score
  → Frontend renders cards with similarity bar
```

### Telegram Save Flow
```
User sends: /save https://example.com #ai #tools my notes
  → TelegramBotService.onUpdateReceived()
      → SEC-4: check chatId against allowedChatId
      → handleSave(args)
          → Parse: URL, tags, content type, notes from args
          → SEC-3: assertNotPrivateUrl(url) — block SSRF
          → Jsoup.connect(url).maxBodySize(500_000).get() — fetch metadata
          → ItemService.quickSave(url, null, title, desc, type, tags, notes)
  → Bot replies with saved item summary
```

---

## 10. Security Hardening (Post-Audit)

All 7 security issues from the audit have been fixed:

| ID | Vulnerability | Fix Applied |
|---|---|---|
| SEC-1 | Error details leaked in prod | `include-message: never` in application.yml |
| SEC-2 | API key reflected in error logs | Response body truncated to 200 chars before logging |
| SEC-3 | SSRF via Jsoup fetch on user URLs | `assertNotPrivateUrl()` — rejects loopback/private/link-local IPs |
| SEC-4 | Open Telegram bot | `TELEGRAM_ALLOWED_CHAT_ID` env var; unauthorized users get ⛔ message |
| SEC-5 | CORS wildcard `allowedHeaders("*")` | Explicit list: `Content-Type`, `Accept`, `Authorization`, `X-Requested-With` |
| SEC-6 | `javascript:` URIs accepted in URL fields | `@Pattern(^(https?://.*)?$)` on `originalUrl` and `sourceUrl` |
| SEC-7 | `content_type` injected into CSS class | Whitelist `Set<String>` validation in app.js and home.js |

---

## 11. Known Gotchas & Design Decisions

### Embedding storage as String
The `embedding` column is `vector(384)` in PostgreSQL but mapped as a `String` in Java. The string format is `"[f1,f2,...,f384]"`. PostgreSQL auto-casts this text literal to the vector type on INSERT/UPDATE. This avoids needing a custom Hibernate type for pgvector.

### Tags as TEXT[]
Tags use `hypersistence-utils StringArrayType` for proper JDBC array handling. In native query results, the JDBC driver may return either `java.sql.Array` or a raw string `"{tag1,tag2}"`. `SearchService.mapRow()` handles both formats.

### No Authentication
The REST API has **no authentication**. It is designed as a private backend (deployed to Railway) with CORS restricting the allowed frontend origin. If you expose the Railway URL publicly, anyone can read/write your data. Consider adding `spring-security` with an API key header if needed.

### Telegram Bot is single-user
The bot is designed for personal use. Set `TELEGRAM_ALLOWED_CHAT_ID` to your Telegram user ID to prevent others from accessing your vault via the bot.

### ddl-auto: validate
Hibernate is set to `validate`, which means **it will not create or modify the database schema**. You must run `sql/setup.sql` manually in Supabase before the first deployment, and manually apply any future schema changes.

### HuggingFace Free Tier
The free tier's model sometimes takes 20–30s to "wake up" (cold start). The code handles 503 responses by sleeping 20s and retrying once. If the model is still loading, the item is saved without embedding (it can be browsed but won't appear in semantic search).

### HNSW vs IVFFlat
The previous IVFFlat index (now replaced) required ~10,000+ rows to work effectively. The HNSW index works well at any dataset size with no minimum row requirement.

### Frontend Module Loading
All JS is loaded as ES Modules. Only `app.js` is in a `<script type="module">` tag in index.html. All other modules are loaded via `import` statements. This means there is no redundant double-loading (BUG-6 from original code was removed).

---

## 12. Deployment Guide

### Step 1: Supabase (Database)
1. Create a new Supabase project
2. Go to SQL Editor → New Query
3. Paste and run the entire contents of `sql/setup.sql`
4. Note your connection string from Settings → Database → Direct Connection

### Step 2: Railway (Backend)
1. Create a new Railway project
2. Connect your GitHub repo
3. Set **root directory** to `backend/`
4. Railway auto-detects Maven; build command: `mvn clean package -DskipTests`
5. Start command: `java -jar target/reelvault.jar`
6. Add all environment variables from [Section 4](#4-environment-variables-complete-list)

### Step 3: Vercel (Frontend)
1. Update `frontend/js/env.js`:
   ```js
   window.REELVAULT_API_URL = 'https://your-railway-url.up.railway.app';
   ```
2. Create a new Vercel project
3. Set **root directory** to `frontend/`
4. No build command needed (static files)
5. Add the Vercel URL to Railway's `CORS_ALLOWED_ORIGIN` env var

### Step 4: Telegram Bot
1. Create a bot via @BotFather on Telegram → get token
2. Message @userinfobot to get your personal chat ID
3. Set `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `TELEGRAM_ALLOWED_CHAT_ID` in Railway

---

## 13. Change Log

### V1 — 2026-04-15 (This Document)
Initial production-ready state after full security and bug audit.

**Security fixes (7):** SEC-1 through SEC-7 — error exposure, SSRF, open bot, CORS wildcard, javascript: URL injection, CSS class injection.

**Bug fixes (8):** BUG-1 (String.valueOf null), BUG-2 (InterruptedException), BUG-3 (active class), BUG-4 (delete handler), BUG-5 (tag-preview null), BUG-6 (redundant scripts), BUG-7 (interval JDBC), BUG-8 (tags array cast).

**Logic fixes (6):** MED-1 (truncate embedding input), MED-2 (Jsoup body limit), MED-3 (vector parse error), MED-4 (URL validation), MED-5 (keydown listener), MED-6 (HNSW index).

**Code quality (4):** LOW-1 (dev profile), LOW-2 (remove setId), LOW-3 (SearchResultDto timestamp), LOW-4 (double delete handler).

---

*Next document: `V2_Document.md` — to be created after the next session that modifies code.*
