"""
LLM Agent for Data Fix Generation
Per meeting notes: 
- Use local LLM agent until API key is available
- Once key available, switch to API (key can be found in ML Core)
- Fetches semantic data from Qdrant and feeds it to LLM with user prompt
"""

import logging
import subprocess
from typing import Optional
from config import DATA_FIX_SYSTEM_PROMPT, LLM_MODE, LLM_API_KEY, LLM_MODEL

logger = logging.getLogger(__name__)


class LLMAgent:
    """
    LLM Agent that generates data fixes based on:
    1. User prompt (description of the data issue)
    2. Fetched similar fixes from Qdrant (context)
    3. System prompt (instructions for generating good fixes)
    
    Supports two modes:
    - "local": Uses locally running Ollama (no API key needed - POC mode)
    - "api": Uses OpenAI/Azure OpenAI API (production mode)
    """

    def __init__(self, mode: str = LLM_MODE, api_key: Optional[str] = LLM_API_KEY, model: str = LLM_MODEL):
        self.mode = mode
        self.api_key = api_key
        self.model = model
        logger.info(f"LLM Agent initialized in '{mode}' mode (model: {model})")

    def generate_data_fix(
        self,
        user_prompt: str,
        similar_fixes: list[dict],
        system_prompt: str = DATA_FIX_SYSTEM_PROMPT
    ) -> str:
        """
        Generate a data fix based on user prompt and similar historical fixes.
        
        Args:
            user_prompt: User's description of the data issue
            similar_fixes: List of similar past fixes retrieved from Qdrant
            system_prompt: Instructions for the LLM
            
        Returns:
            Generated data fix as string
        """
        # Build context from similar fixes retrieved from Qdrant
        context = self._build_context(similar_fixes)

        # Construct full user message
        full_user_message = f"""
## Similar Historical Data Fixes (retrieved from vector database):
{context}

---

## Current Data Issue to Fix:
{user_prompt}

Please generate a data fix script based on the similar historical fixes above and the current issue description.
"""

        if self.mode == "local":
            return self._call_local_llm(system_prompt, full_user_message)
        elif self.mode == "api":
            return self._call_api_llm(system_prompt, full_user_message)
        else:
            raise ValueError(f"Unknown LLM mode: {self.mode}")

    def _build_context(self, similar_fixes: list[dict]) -> str:
        """
        Format similar fixes from Qdrant into readable context for the LLM.
        
        Args:
            similar_fixes: List of Qdrant search result hits
        """
        if not similar_fixes:
            return "No similar historical data fixes found in the database."

        context_parts = []
        for i, hit in enumerate(similar_fixes, 1):
            # Qdrant returns hits with 'payload' and 'score'
            payload = hit.get("payload", hit)  # handle both direct and nested payloads
            score = hit.get("score", "N/A")

            title = payload.get("title", "Untitled")
            source = payload.get("source", "unknown")
            content = payload.get("content", "")
            url = payload.get("url", "")

            context_parts.append(f"""
### Fix #{i} (Similarity: {score:.3f if isinstance(score, float) else score})
- **Source**: {source}
- **Title**: {title}
- **URL**: {url if url else 'N/A'}
- **Content**:
```
{content[:2000]}{'...' if len(content) > 2000 else ''}
```
""")

        return "\n".join(context_parts)

    def _call_local_llm(self, system_prompt: str, user_message: str) -> str:
        """
        Call a locally running LLM via Ollama HTTP API.
        
        Per meeting: use local agent until LLM API key is obtained.
        Make sure Ollama is running: `ollama serve`
        Pull a model: `ollama pull llama3` or `ollama pull codellama`
        """
        import requests
        try:
            # Try Ollama first (most common local LLM runner)
            ollama_url = "http://localhost:11434/api/generate"
            full_prompt = f"System: {system_prompt}\n\nUser: {user_message}\n\nAssistant:"

            response = requests.post(
                ollama_url,
                json={
                    "model": "codellama",  # codellama is best for SQL/code generation
                    "prompt": full_prompt,
                    "stream": False,
                    "options": {
                        "temperature": 0.2,  # Low temp for deterministic code output
                        "num_predict": 2048
                    }
                },
                timeout=120
            )
            response.raise_for_status()
            return response.json().get("response", "No response from local LLM")

        except requests.exceptions.ConnectionError:
            logger.warning("Ollama not running. Falling back to simple template generation.")
            return self._template_fallback(user_message)
        except Exception as e:
            logger.error(f"Local LLM call failed: {e}")
            return self._template_fallback(user_message)

    def _call_api_llm(self, system_prompt: str, user_message: str) -> str:
        """
        Call LLM via OpenAI-compatible API.
        Used once the LLM API key is obtained from ML Core.
        """
        if not self.api_key:
            raise ValueError("LLM API key not set. Please set LLM_API_KEY in config or provide it at runtime.")

        import requests
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

        # Standard OpenAI-compatible format
        # If using Azure OpenAI, update the URL accordingly
        url = "https://api.openai.com/v1/chat/completions"

        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message}
            ],
            "temperature": 0.2,
            "max_tokens": 2048
        }

        logger.info(f"Calling LLM API (model: {self.model})")
        response = requests.post(url, json=payload, headers=headers)
        response.raise_for_status()

        result = response.json()
        return result["choices"][0]["message"]["content"]

    def _template_fallback(self, user_message: str) -> str:
        """
        Simple template-based fallback when no LLM is available.
        Returns a structured template for the engineer to fill in.
        """
        return f"""
## Issue Analysis
[LLM not available - please fill in manually]
Issue Description: {user_message[:500]}

## Proposed Data Fix (SQL/Script)
```sql
-- TODO: Write data fix based on issue description above
-- Reference the similar fixes retrieved from vector DB

BEGIN TRANSACTION;

-- Step 1: Validate the issue exists
SELECT COUNT(*) FROM <table_name> WHERE <condition>;

-- Step 2: Apply the fix
UPDATE <table_name>
SET <column> = <new_value>
WHERE <condition>;
-- Rows affected: N

COMMIT;
-- ROLLBACK; -- uncomment to rollback
```

## Rollback Script
```sql
BEGIN TRANSACTION;
-- Reverse the fix applied above
UPDATE <table_name>
SET <column> = <old_value>
WHERE <condition>;
COMMIT;
```

## Risks & Assumptions
- [TODO: Identify risks]
- [TODO: List assumptions]
- Always test in non-production first
"""
