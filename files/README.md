# Data Fix Generator

RAG-based system that generates data fix scripts using historical fixes from Jira, Confluence, Bitbucket, and local files — stored in a Qdrant vector database via Deutsche Bank's ML-Core APIs.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  INGESTION PHASE (run once / periodically)                  │
│                                                             │
│  Jira ──┐                                                   │
│  Confluence ──┤──► Python Code ──► Create Embedding ──►     │
│  Bitbucket ───┤    (data_sources)   (Embeddings API)        │
│  Local Files ─┘                          │                  │
│                                          ▼                  │
│                                   Store in Qdrant           │
│                                   (Vector DB API)           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  QUERY PHASE (on demand)                                    │
│                                                             │
│  User Prompt                                                │
│      │                                                      │
│      ▼                                                      │
│  Embed Query ──► Search Qdrant ──► Similar Fixes            │
│  (Embeddings API)  (Vector DB API)      │                   │
│                                         ▼                   │
│                              LLM (local Ollama / API)       │
│                              + System Prompt                │
│                                         │                   │
│                                         ▼                   │
│                              Generated Data Fix Script      │
└─────────────────────────────────────────────────────────────┘
```

---

## ML-Core APIs Used

| API | Base URL | Key Endpoints Used |
|-----|----------|--------------------|
| **Embeddings API** | `/api/embeddings` | `POST /input/text` |
| **Vector DB API** | `/api/vectordb` | `POST /collection/create`, `POST /insert/embeddings`, `POST /add/embeddings`, `POST /search/embeddings`, `POST /search` |

---

## Setup

### 1. Install dependencies
```bash
pip install -r requirements.txt
```

### 2. Configure environment variables
```bash
# ML-Core API (leave empty for POC - no auth yet)
export MLCORE_API_KEY=""

# LLM - leave unset to use local Ollama (POC mode)
# Set when API key is available from ML Core
export LLM_API_KEY=""

# Jira
export JIRA_BASE_URL="https://your-instance.atlassian.net"
export JIRA_USER_EMAIL="your.email@db.com"
export JIRA_API_TOKEN="your-jira-token"

# Confluence
export CONFLUENCE_BASE_URL="https://your-instance.atlassian.net"
export CONFLUENCE_USER_EMAIL="your.email@db.com"
export CONFLUENCE_API_TOKEN="your-confluence-token"
export CONFLUENCE_SPACE_KEY="DATA"

# Bitbucket
export BITBUCKET_TOKEN="your-bitbucket-token"
export BITBUCKET_WORKSPACE="your-workspace"
export BITBUCKET_REPO_SLUG="your-repo"

# Local files (for POC)
export LOCAL_DATA_PATH="./local_data_fixes"
```

### 3. (POC) Set up local LLM with Ollama
```bash
# Install Ollama: https://ollama.ai
curl -fsSL https://ollama.ai/install.sh | sh

# Pull codellama (best for SQL/code generation)
ollama pull codellama

# Start Ollama server
ollama serve
```

---

## Usage

### Check API health
```bash
python main.py health
```

### Ingest historical data fixes (run first)
```bash
# From local files (POC)
python main.py ingest --local ./local_data_fixes

# From all configured sources
python main.py ingest

# Force recreate the vector collection
python main.py ingest --local ./local_data_fixes --force-recreate
```

### Generate a data fix

**Interactive mode:**
```bash
python main.py generate
```

**Single prompt:**
```bash
python main.py generate --prompt "Duplicate records in TRADE_BOOKING table where trade_id matches but booking_date differs"

# With output file
python main.py generate \
  --prompt "Null values in POSITION.book_id for APAC region trades from March 2024" \
  --output fix_position_book_id.sql \
  --top-k 5
```

---

## File Structure

```
data_fix_generator/
├── main.py                  # CLI entry point
├── config.py                # API URLs, collection names, LLM config
├── mlcore_client.py         # Embeddings API + Vector DB API clients
├── data_sources.py          # Jira / Confluence / Bitbucket / Local fetchers
├── ingestion_pipeline.py    # Fetch → Embed → Store pipeline
├── data_fix_generator.py    # Query → Search → Generate pipeline
├── llm_agent.py             # LLM wrapper (local Ollama or API)
├── requirements.txt
└── local_data_fixes/        # Place local .sql/.py fix files here
```

---

## Switching from Local LLM to API

Per the 6th March meeting notes: once the LLM API key is available from ML Core:

```bash
export LLM_API_KEY="your-llm-api-key"
python main.py generate  # will automatically use API mode
```

Or in code:
```python
from data_fix_generator import DataFixGenerator

generator = DataFixGenerator(
    api_key="mlcore-key",
    llm_mode="api",           # change from "local" to "api"
    llm_api_key="your-key",
    llm_model="gpt-4"
)
result = generator.generate("Describe your data issue here")
print(result["generated_fix"])
```
