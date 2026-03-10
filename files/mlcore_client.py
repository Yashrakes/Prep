"""
ML-Core API Client
Handles calls to:
- Embeddings API (/api/embeddings) - Generate vector embeddings from text
- Vector Database API (/api/vectordb) - Store and search embeddings in Qdrant
"""

import requests
import logging
from typing import Optional
from config import (
    EMBEDDINGS_API_BASE, VECTOR_DB_API_BASE,
    EMBED_INPUT_TEXT, EMBED_HEALTH,
    VECTORDB_HEALTH, VECTORDB_INSERT, VECTORDB_ADD, VECTORDB_SEARCH,
    VECTORDB_SEARCH_EMBEDDINGS, VECTORDB_COLLECTION_EXIST,
    VECTORDB_COLLECTION_EMPTY, VECTORDB_COLLECTION_CREATE,
    VECTORDB_COLLECTION_DELETE, VECTORDB_DELETE,
    EMBEDDING_MODEL, EMBEDDING_TASK_TYPE
)

logger = logging.getLogger(__name__)


class EmbeddingsAPIClient:
    """
    Client for ML-Core Embeddings API
    Base: /api/embeddings
    Endpoints:
      GET  /health         - Health check
      POST /input/text     - Generate embeddings from text
    """

    def __init__(self, api_key: Optional[str] = None):
        self.base_url = EMBEDDINGS_API_BASE
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        if api_key:
            self.headers["Authorization"] = f"Bearer {api_key}"

    def health_check(self) -> dict:
        """Check if embedding service is healthy"""
        url = f"{self.base_url}{EMBED_HEALTH}"
        response = requests.get(url, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def generate_embedding(self, text: str, task_type: str = EMBEDDING_TASK_TYPE, model: str = EMBEDDING_MODEL) -> list[float]:
        """
        Generate vector embedding for input text
        POST /input/text
        
        Args:
            text: The text to embed
            task_type: RETRIEVAL_DOCUMENT or RETRIEVAL_QUERY
            model: Embedding model name
            
        Returns:
            List of floats representing the embedding vector
        """
        url = f"{self.base_url}{EMBED_INPUT_TEXT}"
        payload = {
            "text": text,
            "task_type": task_type,
            "model": model
        }
        logger.info(f"Generating embedding for text (len={len(text)})")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        result = response.json()
        
        # Extract embedding vector from response
        # Adjust key based on actual API response schema
        embedding = result.get("embedding") or result.get("data", [{}])[0].get("embedding")
        if not embedding:
            raise ValueError(f"No embedding found in response: {result}")
        
        logger.info(f"Generated embedding of dimension {len(embedding)}")
        return embedding


class VectorDBAPIClient:
    """
    Client for ML-Core Vector Database API (Qdrant-backed)
    Base: /api/vectordb
    Endpoints:
      GET  /health                   - Health check
      POST /insert/embeddings        - Insert (overwrite) embeddings into collection
      POST /add/embeddings           - Add embeddings to existing collection
      POST /delete/embeddings        - Delete embeddings by IDs
      POST /search                   - Search by text input
      POST /search/embeddings        - Search by embedding vector
      POST /collection/exist         - Check if collection exists
      POST /collection/empty         - Check if collection is empty
      POST /collection/create        - Create a new collection
      POST /collection/delete        - Delete an existing collection (irreversible)
    """

    def __init__(self, api_key: Optional[str] = None):
        self.base_url = VECTOR_DB_API_BASE
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        if api_key:
            self.headers["Authorization"] = f"Bearer {api_key}"

    def health_check(self) -> dict:
        url = f"{self.base_url}{VECTORDB_HEALTH}"
        response = requests.get(url, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def collection_exists(self, collection_name: str) -> bool:
        """Check whether a collection exists"""
        url = f"{self.base_url}{VECTORDB_COLLECTION_EXIST}"
        payload = {"collection_name": collection_name}
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        result = response.json()
        return result.get("exists", False)

    def collection_is_empty(self, collection_name: str) -> bool:
        """Check whether a collection is empty"""
        url = f"{self.base_url}{VECTORDB_COLLECTION_EMPTY}"
        payload = {"collection_name": collection_name}
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        result = response.json()
        return result.get("empty", True)

    def create_collection(self, collection_name: str, vector_size: int = 1536, distance: str = "Cosine") -> dict:
        """
        Create a new vector collection in Qdrant
        
        Args:
            collection_name: Name of the collection
            vector_size: Dimension of vectors (1536 for OpenAI ada-002)
            distance: Distance metric - Cosine, Euclidean, Dot
        """
        url = f"{self.base_url}{VECTORDB_COLLECTION_CREATE}"
        payload = {
            "collection_name": collection_name,
            "vector_size": vector_size,
            "distance": distance
        }
        logger.info(f"Creating collection: {collection_name}")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def insert_embeddings(self, collection_name: str, points: list[dict]) -> dict:
        """
        Insert embeddings into collection - OVERWRITES existing points with same IDs
        
        Args:
            collection_name: Target collection
            points: List of dicts with keys: id, vector, payload
        """
        url = f"{self.base_url}{VECTORDB_INSERT}"
        payload = {
            "collection_name": collection_name,
            "points": points
        }
        logger.info(f"Inserting {len(points)} points into {collection_name}")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def add_embeddings(self, collection_name: str, points: list[dict]) -> dict:
        """
        Add embeddings to existing collection - does NOT overwrite
        
        Args:
            collection_name: Target collection
            points: List of dicts with keys: id, vector, payload
        """
        url = f"{self.base_url}{VECTORDB_ADD}"
        payload = {
            "collection_name": collection_name,
            "points": points
        }
        logger.info(f"Adding {len(points)} points to {collection_name}")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def delete_embeddings(self, collection_name: str, ids: list) -> dict:
        """Delete specific points by IDs"""
        url = f"{self.base_url}{VECTORDB_DELETE}"
        payload = {
            "collection_name": collection_name,
            "ids": ids
        }
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()

    def search_by_text(self, collection_name: str, query_text: str, top_k: int = 5) -> list[dict]:
        """
        Search vector DB using raw text input (API handles embedding internally)
        POST /search
        """
        url = f"{self.base_url}{VECTORDB_SEARCH}"
        payload = {
            "collection_name": collection_name,
            "query": query_text,
            "top_k": top_k
        }
        logger.info(f"Searching collection {collection_name} with text query (top_k={top_k})")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        result = response.json()
        return result.get("results", result.get("hits", []))

    def search_by_embedding(self, collection_name: str, query_vector: list[float], top_k: int = 5) -> list[dict]:
        """
        Search vector DB using a pre-computed embedding vector
        POST /search/embeddings
        """
        url = f"{self.base_url}{VECTORDB_SEARCH_EMBEDDINGS}"
        payload = {
            "collection_name": collection_name,
            "query_vector": query_vector,
            "top_k": top_k
        }
        logger.info(f"Searching collection {collection_name} by embedding vector (top_k={top_k})")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        result = response.json()
        return result.get("results", result.get("hits", []))

    def delete_collection(self, collection_name: str) -> dict:
        """Delete entire collection - IRREVERSIBLE"""
        url = f"{self.base_url}{VECTORDB_COLLECTION_DELETE}"
        payload = {"collection_name": collection_name}
        logger.warning(f"Deleting collection: {collection_name} - THIS IS IRREVERSIBLE")
        response = requests.post(url, json=payload, headers=self.headers, verify=False)
        response.raise_for_status()
        return response.json()
