"""
Ingestion Pipeline
Step 1: Fetch data fixes from all sources (Jira, Confluence, Bitbucket, Local)
Step 2: Create embeddings via ML-Core Embeddings API
Step 3: Store embeddings in Qdrant via ML-Core Vector DB API

This is the "offline" / setup phase of the architecture.
"""

import uuid
import logging
import time
from typing import Optional
from data_sources import DataFixRecord, fetch_all_sources
from mlcore_client import EmbeddingsAPIClient, VectorDBAPIClient
from config import COLLECTION_NAME, TOP_K_RESULTS, EMBEDDING_TASK_TYPE

logger = logging.getLogger(__name__)


class DataFixIngestionPipeline:
    """
    Ingests historical data fixes into the vector database.
    
    Flow: Sources → Fetch → Embed → Store in Qdrant
    """

    def __init__(self, api_key: Optional[str] = None):
        self.embed_client = EmbeddingsAPIClient(api_key=api_key)
        self.vectordb_client = VectorDBAPIClient(api_key=api_key)
        self.collection_name = COLLECTION_NAME

    def setup_collection(self, vector_size: int = 1536, force_recreate: bool = False) -> bool:
        """
        Ensure the Qdrant collection exists.
        
        Args:
            vector_size: Embedding dimensions (1536 for text-embedding-ada-002)
            force_recreate: If True, delete and recreate the collection
            
        Returns:
            True if collection is ready
        """
        if force_recreate and self.vectordb_client.collection_exists(self.collection_name):
            logger.warning(f"Force recreating collection: {self.collection_name}")
            self.vectordb_client.delete_collection(self.collection_name)
            time.sleep(1)

        if not self.vectordb_client.collection_exists(self.collection_name):
            logger.info(f"Creating collection: {self.collection_name}")
            self.vectordb_client.create_collection(
                collection_name=self.collection_name,
                vector_size=vector_size,
                distance="Cosine"
            )
            logger.info(f"Collection '{self.collection_name}' created successfully")
        else:
            logger.info(f"Collection '{self.collection_name}' already exists")

        return True

    def embed_records(self, records: list[DataFixRecord], batch_size: int = 10) -> list[dict]:
        """
        Generate embeddings for all data fix records.
        
        Args:
            records: List of DataFixRecord objects
            batch_size: Number of records to process before logging progress
            
        Returns:
            List of Qdrant-ready point dicts with {id, vector, payload}
        """
        points = []
        failed = 0

        for i, record in enumerate(records):
            try:
                text = record.to_text_for_embedding()
                vector = self.embed_client.generate_embedding(
                    text=text,
                    task_type=EMBEDDING_TASK_TYPE  # RETRIEVAL_DOCUMENT for storage
                )

                # Generate deterministic UUID from record ID for idempotency
                point_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, record.id))

                points.append({
                    "id": point_id,
                    "vector": vector,
                    "payload": record.to_payload()
                })

                if (i + 1) % batch_size == 0:
                    logger.info(f"Embedded {i + 1}/{len(records)} records...")

            except Exception as e:
                logger.error(f"Failed to embed record {record.id}: {e}")
                failed += 1
                continue

        logger.info(f"Embedding complete: {len(points)} succeeded, {failed} failed")
        return points

    def store_points(self, points: list[dict], mode: str = "add", batch_size: int = 50) -> int:
        """
        Store embedding points in Qdrant in batches.
        
        Args:
            points: List of {id, vector, payload} dicts
            mode: "insert" (overwrite) or "add" (append to existing)
            batch_size: Points to upload per API call
            
        Returns:
            Total number of points stored
        """
        stored = 0
        for i in range(0, len(points), batch_size):
            batch = points[i: i + batch_size]
            try:
                if mode == "insert":
                    self.vectordb_client.insert_embeddings(self.collection_name, batch)
                else:
                    self.vectordb_client.add_embeddings(self.collection_name, batch)
                stored += len(batch)
                logger.info(f"Stored batch {i // batch_size + 1}: {stored}/{len(points)} points")
            except Exception as e:
                logger.error(f"Failed to store batch starting at index {i}: {e}")

        return stored

    def run(
        self,
        jira_config: Optional[dict] = None,
        confluence_config: Optional[dict] = None,
        bitbucket_config: Optional[dict] = None,
        local_path: Optional[str] = None,
        force_recreate: bool = False,
        mode: str = "add"
    ) -> dict:
        """
        Run the full ingestion pipeline end-to-end.
        
        Args:
            jira_config: Jira connection config (or None to skip)
            confluence_config: Confluence connection config (or None to skip)
            bitbucket_config: Bitbucket connection config (or None to skip)
            local_path: Path to local fix files (or None to skip)
            force_recreate: Wipe and recreate the Qdrant collection
            mode: "add" or "insert" for vector DB storage
            
        Returns:
            Summary dict with counts
        """
        logger.info("=" * 60)
        logger.info("Starting Data Fix Ingestion Pipeline")
        logger.info("=" * 60)

        # Step 1: Check API health
        logger.info("Checking API health...")
        try:
            self.embed_client.health_check()
            logger.info("✓ Embeddings API healthy")
        except Exception as e:
            logger.error(f"✗ Embeddings API unhealthy: {e}")
            raise

        try:
            self.vectordb_client.health_check()
            logger.info("✓ Vector DB API healthy")
        except Exception as e:
            logger.error(f"✗ Vector DB API unhealthy: {e}")
            raise

        # Step 2: Setup collection
        self.setup_collection(force_recreate=force_recreate)

        # Step 3: Fetch from all sources
        logger.info("\nFetching data fixes from all sources...")
        records = fetch_all_sources(
            jira_config=jira_config,
            confluence_config=confluence_config,
            bitbucket_config=bitbucket_config,
            local_path=local_path
        )

        if not records:
            logger.warning("No records fetched from any source. Exiting.")
            return {"fetched": 0, "embedded": 0, "stored": 0}

        # Step 4: Generate embeddings
        logger.info(f"\nGenerating embeddings for {len(records)} records...")
        points = self.embed_records(records)

        # Step 5: Store in Qdrant
        logger.info(f"\nStoring {len(points)} vectors in Qdrant...")
        stored = self.store_points(points, mode=mode)

        summary = {
            "fetched": len(records),
            "embedded": len(points),
            "stored": stored
        }

        logger.info("\n" + "=" * 60)
        logger.info(f"Ingestion Complete: {summary}")
        logger.info("=" * 60)
        return summary
