# ReelVault — Personal Semantic Knowledge Base

> Save links, reels, articles, and notes from anywhere. Search your vault using natural language powered by AI vector embeddings.

**Stack:** Java 21 + Spring Boot 3.2 · PostgreSQL + pgvector (Supabase) · Hugging Face Embeddings · Telegram Bot · Vanilla HTML/CSS/JS

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Step 1 — Supabase Database Setup](#step-1--supabase-database-setup)
3. [Step 2 — Hugging Face API Key](#step-2--hugging-face-api-key)
4. [Step 3 — Telegram Bot Registration](#step-3--telegram-bot-registration)
5. [Step 4 — Local Development](#step-4--local-development)
6. [Step 5 — Deploy Backend to Railway](#step-5--deploy-backend-to-railway)
7. [Step 6 — Deploy Frontend to Vercel](#step-6--deploy-frontend-to-vercel)
8. [API Reference](#api-reference)
9. [Environment Variables Reference](#environment-variables-reference)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌──────────────┐    HTTPS     ┌────────────────────────────┐
│  Vercel      │ ──────────▶ │  Railway.app               │
│  (Frontend)  │             │  Spring Boot 3.2 (Java 21) │
│  HTML/CSS/JS │ ◀────────── │  + Telegram Bot (built-in) │
└──────────────┘             └────────────┬───────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
             ┌──────▼──────┐     ┌────── ▼──────┐    ┌────────▼──────┐
             │  Supabase   │     │ Hugging Face │    │   Telegram    │
             │  PostgreSQL │     │ Inference API│    │   Server      │
             │  + pgvector │     │ (free tier)  │    │               │
             └─────────────┘     └──────────────┘    └───────────────┘
```

---

## Step 1 — Supabase Database Setup

1. Go to [supabase.com](https://supabase.com) and create a **free account**.
2. Click **New Project**. Give it a name (e.g. `reelvault`) and set a strong database password. Save this password!
3. Wait ~2 minutes for the project to provision.
4. Open the **SQL Editor** (left sidebar → SQL Editor → + New Query).
5. Paste the entire contents of [`sql/setup.sql`](./sql/setup.sql) and click **Run**.
6. You should see: `Success. No rows returned.`

### Get your connection credentials

In your Supabase project:
- Go to **Settings → Database**
- Scroll to **Connection string** → select **JDBC**
- Copy the URL. It looks like:
  ```
  jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres
  ```
- Add `?sslmode=require` to the end:
  ```
  jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres?sslmode=require
  ```

You'll need:
| Variable | Where to find |
|---|---|
| `SUPABASE_DB_URL` | JDBC URL (above) |
| `SUPABASE_DB_USER` | Usually `postgres` |
| `SUPABASE_DB_PASSWORD` | The password you set when creating the project |

---

## Step 2 — Hugging Face API Key

1. Create a free account at [huggingface.co](https://huggingface.co)
2. Go to **Settings → Access Tokens**
3. Click **New token** → name it `reelvault` → type: **Read** → Generate
4. Copy the token (starts with `hf_...`). This is your `HUGGINGFACE_API_KEY`.

> **Free tier note:** The Hugging Face free tier may put the model to sleep. The first request after inactivity takes ~20 seconds (the backend retries automatically). Subsequent requests are fast.

---

## Step 3 — Telegram Bot Registration

1. Open Telegram and search for **@BotFather**
2. Send `/newbot`
3. Follow the prompts:
   - Bot name: `ReelVault` (or anything you like)
   - Bot username: `YourReelVaultBot` (must end in `bot`)
4. BotFather will send you a token like: `7123456789:AAH...`
5. Save this as `TELEGRAM_BOT_TOKEN`
6. Save the username (without `@`) as `TELEGRAM_BOT_USERNAME`, e.g. `YourReelVaultBot`

### Enable commands in BotFather (optional but recommended)

Send `/setcommands` to @BotFather, select your bot, then paste:
```
save - Save a URL to your vault
search - Search with natural language
recent - Show last 5 saved items
digest - Items saved in last 24 hours
list - Filter by tag
help - Show all commands
```

---

## Step 4 — Local Development

### Prerequisites
- Java 21 (via `sdk install java 21`)
- Maven 3.9+ (`brew install maven` on Mac)
- A modern browser for the frontend

### Backend

1. Copy and set environment variables in your shell:
   ```bash
   export SUPABASE_DB_URL="jdbc:postgresql://db.XXX.supabase.co:5432/postgres?sslmode=require"
   export SUPABASE_DB_USER="postgres"
   export SUPABASE_DB_PASSWORD="your-supabase-password"
   export HUGGINGFACE_API_KEY="hf_xxxx"
   export TELEGRAM_BOT_TOKEN="your-telegram-token"
   export TELEGRAM_BOT_USERNAME="YourBotUsername"
   export CORS_ALLOWED_ORIGIN="http://localhost:5500"
   ```

2. Build and run:
   ```bash
   cd backend
   mvn clean spring-boot:run
   ```
   The backend starts on `http://localhost:8080`.

### Frontend

Open `frontend/index.html` directly in a browser, or use a local server:
```bash
cd frontend
# Option 1: Python
python3 -m http.server 5500

# Option 2: VS Code Live Server extension (recommended)
# Right-click index.html → Open with Live Server
```

Open `http://localhost:5500`.

> **Note:** For local dev, `CORS_ALLOWED_ORIGIN` must match your frontend origin (e.g. `http://localhost:5500` or `http://127.0.0.1:5500`).

---

## Step 5 — Deploy Backend to Railway

### Initial setup

1. Create a free account at [railway.app](https://railway.app)
2. Click **New Project → Deploy from GitHub repo**
3. Connect your GitHub account and select your repository
4. Railway will auto-detect the project. Set the **Root Directory** to `backend/`
5. Set **build command** (Railway usually auto-detects Maven):
   ```
   mvn clean package -DskipTests
   ```
6. Set **start command**:
   ```
   java -jar target/reelvault.jar
   ```

### Environment Variables

In Railway dashboard → your service → **Variables**, add:

| Variable | Value |
|---|---|
| `SUPABASE_DB_URL` | `jdbc:postgresql://db.XXX.supabase.co:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USER` | `postgres` |
| `SUPABASE_DB_PASSWORD` | your Supabase password |
| `HUGGINGFACE_API_KEY` | `hf_xxxx` |
| `TELEGRAM_BOT_TOKEN` | your bot token |
| `TELEGRAM_BOT_USERNAME` | your bot username (no @) |
| `CORS_ALLOWED_ORIGIN` | `https://your-app.vercel.app` (update after Vercel deploy) |

### Get your Railway URL

After deploy: **Settings → Domains** → copy the URL, e.g. `https://reelvault-production.up.railway.app`

---

## Step 6 — Deploy Frontend to Vercel

### Prepare

1. Edit `frontend/js/env.js` and set your Railway URL:
   ```javascript
   window.REELVAULT_API_URL = 'https://reelvault-production.up.railway.app';
   ```
2. Commit and push to GitHub.

### Deploy

1. Go to [vercel.com](https://vercel.com) and create a free account
2. Click **New Project** → Import your GitHub repo
3. Set **Root Directory** to `frontend/`
4. **Framework Preset**: choose **Other** (plain HTML)
5. Click **Deploy**

Vercel will give you a URL like `https://reelvault.vercel.app`.

### Update CORS

Go back to Railway → Variables → update `CORS_ALLOWED_ORIGIN` to your exact Vercel URL.

---

## API Reference

All endpoints are prefixed with `/api`.

### Items

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/items` | Save a new item (auto-generates embedding) |
| `GET` | `/api/items` | List items (filters: `contentType`, `tag`, `dateFrom`, `dateTo`) |
| `GET` | `/api/items/recent` | Last 10 saved items |
| `GET` | `/api/items/{id}` | Get item by UUID |
| `PUT` | `/api/items/{id}` | Update item |
| `DELETE` | `/api/items/{id}` | Delete item |

### Search

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/search` | Semantic search with similarity scores |

### Example: Save an item

```bash
curl -X POST https://your-backend.railway.app/api/items \
  -H "Content-Type: application/json" \
  -d '{
    "title": "LangGraph — Build stateful AI agents",
    "description": "A library for building multi-actor, stateful AI workflows",
    "original_url": "https://github.com/langchain-ai/langgraph",
    "content_type": "tool",
    "tags": ["AI", "agents", "python", "langchain"],
    "personal_notes": "Saw this on an Instagram reel about AI coding tools"
  }'
```

### Example: Semantic search

```bash
curl -X POST https://your-backend.railway.app/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "AI that can browse the web autonomously", "limit": 5}'
```

**Response:**
```json
[
  {
    "id": "...",
    "title": "LangGraph — Build stateful AI agents",
    "similarity": 0.87,
    "content_type": "tool",
    "tags": ["AI", "agents"],
    "original_url": "https://github.com/langchain-ai/langgraph"
  }
]
```

### Example: Filter by type and tag

```bash
curl "https://your-backend.railway.app/api/items?contentType=video&tag=python&dateFrom=2024-01-01T00:00:00Z"
```

---

## Environment Variables Reference

### Backend (Railway)

| Variable | Required | Description |
|---|---|---|
| `SUPABASE_DB_URL` | ✅ | JDBC connection URL with `?sslmode=require` |
| `SUPABASE_DB_USER` | ✅ | Database username |
| `SUPABASE_DB_PASSWORD` | ✅ | Database password |
| `HUGGINGFACE_API_KEY` | ✅ | HuggingFace API token |
| `TELEGRAM_BOT_TOKEN` | ✅ | Token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | ✅ | Bot username without `@` |
| `CORS_ALLOWED_ORIGIN` | ✅ | Exact Vercel frontend URL |
| `PORT` | ⚡ auto | Railway sets this automatically |

### Frontend (env.js)

| Variable | Description |
|---|---|
| `window.REELVAULT_API_URL` | Your Railway backend URL |

---

## Troubleshooting

### `ddl-auto: validate` error on startup

The `application.yml` is set to `validate` — it expects the table to exist.
**Fix:** Run `sql/setup.sql` in Supabase SQL Editor before starting the backend.

### HuggingFace returns 503

The model is loading (cold start on free tier). The backend automatically waits 20 seconds and retries once. If it fails again, the item is saved without an embedding (it will appear in Browse but not in semantic search results).

### Telegram bot: `TelegramApiException`

- Verify `TELEGRAM_BOT_TOKEN` is correct
- Ensure the token has no extra spaces
- Confirm the bot is not already running in another process

### CORS error in browser

- Verify `CORS_ALLOWED_ORIGIN` in Railway exactly matches your Vercel URL (no trailing slash)
- Use `https://` not `http://` for production

### `vector` type not found in PostgreSQL

You forgot to run:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
This is included in `sql/setup.sql`. Run it in Supabase SQL Editor.

### Embedding dimension mismatch

This app uses `sentence-transformers/all-MiniLM-L6-v2` which produces **384-dimensional** embeddings. The `vector(384)` column definition must match. Do not change the model without also changing the SQL schema and index.

### Railway build fails

Ensure:
1. Root Directory is set to `backend/`
2. Java version is 21 (add `JAVA_VERSION=21` env var in Railway if needed)
3. Build command: `mvn clean package -DskipTests`
4. Start command: `java -jar target/reelvault.jar`

---

## Project Structure

```
reelvault/
├── backend/
│   ├── src/main/java/com/reelvault/
│   │   ├── ReelVaultApplication.java
│   │   ├── config/        CorsConfig.java
│   │   ├── controller/    ItemController.java, SearchController.java
│   │   ├── dto/           ItemRequest.java, SearchRequest.java, SearchResultDto.java
│   │   ├── model/         KnowledgeItem.java
│   │   ├── repository/    KnowledgeItemRepository.java
│   │   ├── service/       ItemService.java, EmbeddingService.java,
│   │   │                  SearchService.java, TelegramBotService.java
│   │   └── util/          VectorUtils.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
│
├── frontend/
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── env.js       ← set REELVAULT_API_URL here
│       ├── api.js
│       ├── app.js
│       ├── home.js
│       ├── browse.js
│       └── add.js
│
├── sql/
│   └── setup.sql
│
└── README.md
```

---

## Telegram Bot Commands

| Command | Example | Description |
|---|---|---|
| `/save [url]` | `/save https://github.com/user/repo` | Auto-fetch title and save |
| `/save [url] - [note]` | `/save https://... - Great ML tool` | Save with personal note |
| `/search [query]` | `/search AI agent browser` | Semantic search, top 5 results |
| `/recent` | `/recent` | Last 5 saved items |
| `/digest` | `/digest` | Items from last 24 hours |
| `/list [tag]` | `/list python` | Filter by tag |
| `/help` | `/help` | Show all commands |

---

Made with ❤️ — ReelVault, your personal AI-powered knowledge base.
# ReelVault
