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
6. [Step 5 — Deploy Backend to Render](#step-5--deploy-backend-to-render)
7. [Step 6 — Deploy Frontend to Vercel](#step-6--deploy-frontend-to-vercel)
8. [API Reference](#api-reference)
9. [Environment Variables Reference](#environment-variables-reference)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌──────────────┐    HTTPS     ┌────────────────────────────┐
│  Vercel      │ ──────────▶ │  Render (Docker)            │
│  (Frontend)  │             │  Spring Boot 3.2 (Java 21)  │
│  HTML/CSS/JS │ ◀────────── │  + Telegram Bot (built-in)  │
└──────────────┘             └────────────┬───────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
             ┌──────▼──────┐     ┌───────▼──────┐    ┌────────▼──────┐
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

### Get your Telegram Chat ID (for security)

1. Open Telegram and search for **@userinfobot**
2. Send `/start` — it replies with your user ID (a number like `987654321`)
3. Save this as `TELEGRAM_ALLOWED_CHAT_ID`

> This restricts the bot to only accept commands from your account.

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
   export TELEGRAM_ALLOWED_CHAT_ID="your-telegram-user-id"
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

## Step 5 — Deploy Backend to Render

ReelVault deploys to [Render](https://render.com) using a **Dockerfile** (Render doesn't support Java natively).

### How the Dockerfile works

```
Stage 1: maven:3.9-eclipse-temurin-21    →  Builds reelvault.jar
Stage 2: eclipse-temurin:21-jre-alpine   →  Lightweight runtime (~200MB)
```

- Dependencies are cached in a separate Docker layer (fast rebuilds)
- JVM is tuned for Render's free tier: `-Xms128m -Xmx384m`
- Port is set via Render's `PORT` env var (defaults to 10000)

### Deploy Steps

1. Push your code to GitHub (if you haven't already):
   ```bash
   git add .
   git commit -m "deploy to Render"
   git push
   ```

2. Go to [render.com](https://render.com) → **New** → **Web Service**

3. Connect your GitHub repo: `kshirsagar-omkar/ReelVault`

4. Render auto-detects the `Dockerfile` at the repo root. Configure:
   | Setting | Value |
   |---|---|
   | **Name** | `reelvault` |
   | **Region** | Pick closest to you |
   | **Branch** | `main` |
   | **Root Directory** | *(leave blank)* |
   | **Runtime** | Docker |
   | **Instance Type** | Free |

5. Add **Environment Variables** in the Render dashboard:

   | Variable | Value |
   |---|---|
   | `SUPABASE_DB_URL` | `jdbc:postgresql://db.XXX.supabase.co:5432/postgres?sslmode=require` |
   | `SUPABASE_DB_USER` | `postgres` |
   | `SUPABASE_DB_PASSWORD` | your Supabase password |
   | `HUGGINGFACE_API_KEY` | `hf_xxxx` |
   | `TELEGRAM_BOT_TOKEN` | your bot token |
   | `TELEGRAM_BOT_USERNAME` | your bot username (no @) |
   | `TELEGRAM_ALLOWED_CHAT_ID` | your Telegram user ID |
   | `CORS_ALLOWED_ORIGIN` | `https://your-app.vercel.app` (update after Vercel deploy) |

6. Click **Create Web Service** → Render builds the Docker image and deploys.

### Get your Render URL

After deploy, your service URL will look like:
```
https://reelvault.onrender.com
```

> **Free tier note:** Render free tier spins down after 15 minutes of inactivity. The first request after that takes ~30-60 seconds (Docker container restart). Consider the Starter plan ($7/month) for always-on.

---

## Step 6 — Deploy Frontend to Vercel

### Prepare

1. Edit `frontend/js/env.js` and set your Render URL:
   ```javascript
   window.REELVAULT_API_URL = 'https://reelvault.onrender.com';
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

Go back to Render → Environment → update `CORS_ALLOWED_ORIGIN` to your exact Vercel URL (no trailing slash).

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
curl -X POST https://reelvault.onrender.com/api/items \
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
curl -X POST https://reelvault.onrender.com/api/search \
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
curl "https://reelvault.onrender.com/api/items?contentType=video&tag=python&dateFrom=2024-01-01T00:00:00Z"
```

---

## Environment Variables Reference

### Backend (Render)

| Variable | Required | Description |
|---|---|---|
| `SUPABASE_DB_URL` | ✅ | JDBC connection URL with `?sslmode=require` |
| `SUPABASE_DB_USER` | ✅ | Database username |
| `SUPABASE_DB_PASSWORD` | ✅ | Database password |
| `HUGGINGFACE_API_KEY` | ✅ | HuggingFace API token |
| `TELEGRAM_BOT_TOKEN` | ✅ | Token from @BotFather |
| `TELEGRAM_BOT_USERNAME` | ✅ | Bot username without `@` |
| `TELEGRAM_ALLOWED_CHAT_ID` | ✅ | Your Telegram user ID (from @userinfobot) |
| `CORS_ALLOWED_ORIGIN` | ✅ | Exact Vercel frontend URL |
| `PORT` | ⚡ auto | Render sets this automatically (default: 10000) |

### Frontend (env.js)

| Variable | Description |
|---|---|
| `window.REELVAULT_API_URL` | Your Render backend URL |

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

- Verify `CORS_ALLOWED_ORIGIN` in Render exactly matches your Vercel URL (no trailing slash)
- Use `https://` not `http://` for production

### `vector` type not found in PostgreSQL

You forgot to run:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
This is included in `sql/setup.sql`. Run it in Supabase SQL Editor.

### Embedding dimension mismatch

This app uses `sentence-transformers/all-MiniLM-L6-v2` which produces **384-dimensional** embeddings. The `vector(384)` column definition must match. Do not change the model without also changing the SQL schema and index.

### Render build fails

Ensure:
1. `Dockerfile` is at the repo root (not inside `backend/`)
2. Root Directory in Render is left blank
3. Runtime is set to **Docker**
4. All environment variables are set before the first deploy

### Render free tier cold starts

Render free tier spins down after 15 min of inactivity. Options:
1. Use a cron service (like [cron-job.org](https://cron-job.org)) to ping your URL every 14 minutes
2. Upgrade to Render Starter plan ($7/month) for always-on

---

## Project Structure

```
ReelVault/
├── Dockerfile              ← Multi-stage Docker build for Render
├── .dockerignore
├── .gitignore
│
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/reelvault/
│       │   ├── ReelVaultApplication.java
│       │   ├── config/        CorsConfig.java
│       │   ├── controller/    ItemController.java, SearchController.java
│       │   ├── dto/           ItemRequest.java, SearchRequest.java, SearchResultDto.java
│       │   ├── model/         KnowledgeItem.java
│       │   ├── repository/    KnowledgeItemRepository.java
│       │   ├── service/       ItemService.java, EmbeddingService.java,
│       │   │                  SearchService.java, TelegramBotService.java
│       │   └── util/          VectorUtils.java
│       └── resources/application.yml
│
├── frontend/
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── env.js            ← Set REELVAULT_API_URL here
│       ├── api.js
│       ├── app.js
│       ├── home.js
│       ├── browse.js
│       └── add.js
│
└── sql/
    └── setup.sql
```

---

## Telegram Bot Commands

| Command | Example | Description |
|---|---|---|
| `/save [url]` | `/save https://github.com/user/repo` | Auto-fetch title and save |
| `/save [url] #tag1 #tag2 note` | `/save https://... #ai #tools Great ML tool` | Save with tags and note |
| `/search [query]` | `/search AI agent browser` | Semantic search, top 5 results |
| `/recent` | `/recent` | Last 10 saved items |
| `/digest` | `/digest` | Items from last 24 hours |
| `/list #tag` or `/list type:video` | `/list #python` | Filter by tag or type |
| `/help` | `/help` | Show all commands |

---

## Security Measures

- **SSRF Protection** — Bot blocks requests to private/loopback IP ranges
- **Telegram Authorization** — Bot only accepts commands from `TELEGRAM_ALLOWED_CHAT_ID`
- **XSS Prevention** — URL fields validated to allow only `http/https` schemes
- **Error Sanitization** — Stack traces and internal messages never exposed in production
- **CORS** — Restricted to explicit frontend origin with explicit allowed headers
- **Content Type Validation** — Whitelist enforcement on both backend and frontend

---

Made with ❤️ — ReelVault, your personal AI-powered knowledge base.
