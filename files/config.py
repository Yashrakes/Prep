"""
Configuration for Data Fix Generator
Based on ML-Core API infrastructure (Deutsche Bank internal)
"""

# ML-Core API Base URLs
EMBEDDINGS_API_BASE = "https://api.dev1-ew4.mlcore.dev.gcp.db.com/api/embeddings"
VECTOR_DB_API_BASE = "https://api.dev1-ew4.mlcore.dev.gcp.db.com/api/vectordb"

# API Endpoints - Embeddings Service
EMBED_HEALTH = "/health"
EMBED_INPUT_TEXT = "/input/text"

# API Endpoints - Vector DB Service
VECTORDB_HEALTH = "/health"
VECTORDB_INSERT = "/insert/embeddings"
VECTORDB_ADD = "/add/embeddings"
VECTORDB_DELETE = "/delete/embeddings"
VECTORDB_SEARCH = "/search"
VECTORDB_SEARCH_EMBEDDINGS = "/search/embeddings"
VECTORDB_COLLECTION_EXIST = "/collection/exist"
VECTORDB_COLLECTION_EMPTY = "/collection/empty"
VECTORDB_COLLECTION_CREATE = "/collection/create"
VECTORDB_COLLECTION_SETUP_IMAGE_RAG = "/collection/setup_image_rag"
VECTORDB_COLLECTION_DELETE = "/collection/delete"

# Collection name in Qdrant (vector DB)
COLLECTION_NAME = "data_fixes"

# LLM Config - using local agent until API key is obtained
# Per meeting notes: "we will try to fetch semantic data from qdrant and give the fetched data to LLM local agent"
LLM_MODE = "local"  # "local" or "api" - switch to "api" once LLM key is available
LLM_API_KEY = None  # Set when available
LLM_MODEL = "gpt-4"  # Target model when API key is available

# Data Source Configs
JIRA_BASE_URL = "https://your-jira-instance.atlassian.net"
JIRA_API_TOKEN = None  # Set via env var JIRA_API_TOKEN
JIRA_USER_EMAIL = None  # Set via env var JIRA_USER_EMAIL

CONFLUENCE_BASE_URL = "https://your-confluence-instance.atlassian.net"
CONFLUENCE_API_TOKEN = None  # Set via env var CONFLUENCE_API_TOKEN

BITBUCKET_BASE_URL = "https://api.bitbucket.org/2.0"
BITBUCKET_TOKEN = None  # Set via env var BITBUCKET_TOKEN

# Local machine data path
LOCAL_DATA_PATH = "./local_data_fixes"

# Embedding model (used by ML-Core embedding service)
EMBEDDING_MODEL = "text-embedding-ada-002"  # or as configured in ML-Core
EMBEDDING_TASK_TYPE = "RETRIEVAL_DOCUMENT"

# Search config
TOP_K_RESULTS = 5  # Number of similar data fixes to fetch from vector DB

# System prompt for data fix generation
DATA_FIX_SYSTEM_PROMPT = """You are a data fix generator assistant. 
Your role is to generate precise SQL/script-based data fixes based on:
1. Historical data fixes retrieved from the vector database (past fixes done for similar issues)
2. The user's description of the current data issue
3. Context about the system/tables involved

Always:
- Validate the fix is safe (prefer UPDATE over DELETE)
- Add WHERE clauses to limit scope
- Include rollback scripts where possible
- Add comments explaining the fix
- Flag any risks or assumptions

Format your response as:
## Issue Analysis
## Proposed Data Fix (SQL/Script)
## Rollback Script
## Risks & Assumptions
"""
