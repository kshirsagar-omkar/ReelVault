# Graph Report - .  (2026-04-17)

## Corpus Check
- Corpus is ~22,223 words - fits in a single context window. You may not need a graph.

## Summary
- 204 nodes · 347 edges · 15 communities detected
- Extraction: 72% EXTRACTED · 28% INFERRED · 0% AMBIGUOUS · INFERRED: 98 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Core Business Logic|Core Business Logic]]
- [[_COMMUNITY_Search Result DTO|Search Result DTO]]
- [[_COMMUNITY_Telegram Bot Integration|Telegram Bot Integration]]
- [[_COMMUNITY_REST API & Repository|REST API & Repository]]
- [[_COMMUNITY_Frontend SPA Router|Frontend SPA Router]]
- [[_COMMUNITY_Item Request DTO|Item Request DTO]]
- [[_COMMUNITY_Add Item Form|Add Item Form]]
- [[_COMMUNITY_Browse & Delete UI|Browse & Delete UI]]
- [[_COMMUNITY_API Client Module|API Client Module]]
- [[_COMMUNITY_Search Controller & Request|Search Controller & Request]]
- [[_COMMUNITY_Embedding Service|Embedding Service]]
- [[_COMMUNITY_Vector Utilities|Vector Utilities]]
- [[_COMMUNITY_Application Entry Point|Application Entry Point]]
- [[_COMMUNITY_CORS Configuration|CORS Configuration]]
- [[_COMMUNITY_Environment Config|Environment Config]]

## God Nodes (most connected - your core abstractions)
1. `SearchResultDto` - 24 edges
2. `KnowledgeItem` - 24 edges
3. `TelegramBotService` - 18 edges
4. `ItemRequest` - 15 edges
5. `ItemService` - 11 edges
6. `loadItems()` - 10 edges
7. `handleSubmit()` - 10 edges
8. `request()` - 9 edges
9. `ItemController` - 8 edges
10. `EmbeddingService` - 8 edges

## Surprising Connections (you probably didn't know these)
- `navigateTo()` --calls--> `initBrowsePage()`  [INFERRED]
  frontend/js/app.js → frontend/js/browse.js
- `navigateTo()` --calls--> `initAddPage()`  [INFERRED]
  frontend/js/app.js → frontend/js/add.js
- `handleSubmit()` --calls--> `saveItem()`  [INFERRED]
  frontend/js/add.js → frontend/js/api.js
- `handleSubmit()` --calls--> `showToast()`  [INFERRED]
  frontend/js/add.js → frontend/js/app.js
- `navigateTo()` --calls--> `initHomePage()`  [INFERRED]
  frontend/js/app.js → frontend/js/home.js

## Communities

### Community 0 - "Core Business Logic"
Cohesion: 0.13
Nodes (2): KnowledgeItem, SearchService

### Community 1 - "Search Result DTO"
Cohesion: 0.08
Nodes (1): SearchResultDto

### Community 2 - "Telegram Bot Integration"
Cohesion: 0.17
Nodes (1): TelegramBotService

### Community 3 - "REST API & Repository"
Cohesion: 0.11
Nodes (3): ItemController, ItemService, KnowledgeItemRepository

### Community 4 - "Frontend SPA Router"
Cohesion: 0.17
Nodes (18): initAddPage(), closeModal(), escapeClose(), escapeHtml(), formatDate(), handleHashChange(), navigateTo(), openItemModal() (+10 more)

### Community 5 - "Item Request DTO"
Cohesion: 0.12
Nodes (1): ItemRequest

### Community 6 - "Add Item Form"
Cohesion: 0.28
Nodes (11): buildPayload(), clearFeedback(), clearFieldErrors(), handleSubmit(), hideElement(), resetForm(), setFieldError(), setSubmitting() (+3 more)

### Community 7 - "Browse & Delete UI"
Cohesion: 0.32
Nodes (11): showToast(), clearFilters(), confirmDelete(), getFilters(), handleGridClick(), initBrowsePage(), loadItems(), renderGrid() (+3 more)

### Community 8 - "API Client Module"
Cohesion: 0.38
Nodes (9): deleteItem(), getBaseUrl(), getItem(), getRecentItems(), listItems(), request(), saveItem(), searchItems() (+1 more)

### Community 9 - "Search Controller & Request"
Cohesion: 0.22
Nodes (2): SearchController, SearchRequest

### Community 10 - "Embedding Service"
Cohesion: 0.39
Nodes (1): EmbeddingService

### Community 11 - "Vector Utilities"
Cohesion: 0.5
Nodes (1): VectorUtils

### Community 12 - "Application Entry Point"
Cohesion: 0.67
Nodes (1): ReelVaultApplication

### Community 13 - "CORS Configuration"
Cohesion: 0.67
Nodes (1): CorsConfig

### Community 14 - "Environment Config"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **Thin community `Environment Config`** (1 nodes): `env.js`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `showToast()` connect `Browse & Delete UI` to `Frontend SPA Router`, `Add Item Form`?**
  _High betweenness centrality (0.260) - this node is a cross-community bridge._
- **Why does `loadItems()` connect `Browse & Delete UI` to `REST API & Repository`?**
  _High betweenness centrality (0.248) - this node is a cross-community bridge._
- **Should `Core Business Logic` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Search Result DTO` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Should `REST API & Repository` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._
- **Should `Item Request DTO` be split into smaller, more focused modules?**
  _Cohesion score 0.12 - nodes in this community are weakly interconnected._