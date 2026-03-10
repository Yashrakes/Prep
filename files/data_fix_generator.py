"""
Data Fix Generator - Query Pipeline
This is the "online" / inference phase of the architecture.

Flow: User Prompt → Generate Query Embedding → Search Qdrant → Feed to LLM → Output Fix

Per architecture diagram:
1. Based on user prompt
2. Fetch data from Qdrant
3. Create data fix based on user context + fetched data + system prompt
"""

import logging
from typing import Optional
from mlcore_client import EmbeddingsAPIClient, VectorDBAPIClient
from llm_agent import LLMAgent
from config import (
    COLLECTION_NAME, TOP_K_RESULTS,
    LLM_MODE, LLM_API_KEY, LLM_MODEL
)

logger = logging.getLogger(__name__)


class DataFixGenerator:
    """
    Main entry point for generating data fixes on demand.
    
    Given a user's description of a data issue, this:
    1. Embeds the query using ML-Core Embeddings API
    2. Searches Qdrant for similar historical fixes
    3. Passes found fixes + user prompt to LLM
    4. Returns generated data fix script
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        llm_mode: str = LLM_MODE,
        llm_api_key: Optional[str] = LLM_API_KEY,
        llm_model: str = LLM_MODEL
    ):
        self.embed_client = EmbeddingsAPIClient(api_key=api_key)
        self.vectordb_client = VectorDBAPIClient(api_key=api_key)
        self.llm_agent = LLMAgent(mode=llm_mode, api_key=llm_api_key, model=llm_model)
        self.collection_name = COLLECTION_NAME

    def generate(
        self,
        user_prompt: str,
        top_k: int = TOP_K_RESULTS,
        search_mode: str = "embedding"  # "embedding" or "text"
    ) -> dict:
        """
        Generate a data fix for the described issue.
        
        Args:
            user_prompt: Natural language description of the data issue
            top_k: Number of similar fixes to retrieve from vector DB
            search_mode: 
                "embedding" - generate embedding then search (more accurate)
                "text"      - send raw text to /search endpoint (simpler)
                
        Returns:
            Dict with keys:
                - user_prompt: original prompt
                - similar_fixes: list of similar fixes from Qdrant
                - generated_fix: the LLM-generated data fix
        """
        logger.info("=" * 60)
        logger.info("Data Fix Generator - Processing Request")
        logger.info(f"User Prompt: {user_prompt[:200]}...")
        logger.info("=" * 60)

        # Step 1: Search Qdrant for similar historical fixes
        logger.info(f"\nSearching vector DB for similar fixes (top_k={top_k}, mode={search_mode})...")

        similar_fixes = []
        try:
            if search_mode == "embedding":
                # Generate embedding for the query (use RETRIEVAL_QUERY task type)
                query_vector = self.embed_client.generate_embedding(
                    text=user_prompt,
                    task_type="RETRIEVAL_QUERY"
                )
                similar_fixes = self.vectordb_client.search_by_embedding(
                    collection_name=self.collection_name,
                    query_vector=query_vector,
                    top_k=top_k
                )
            else:
                # Direct text search (API handles embedding internally)
                similar_fixes = self.vectordb_client.search_by_text(
                    collection_name=self.collection_name,
                    query_text=user_prompt,
                    top_k=top_k
                )

            logger.info(f"Found {len(similar_fixes)} similar historical fixes")
            for i, fix in enumerate(similar_fixes, 1):
                payload = fix.get("payload", fix)
                title = payload.get("title", "Untitled")
                score = fix.get("score", "N/A")
                logger.info(f"  {i}. [{score:.3f}] {title}" if isinstance(score, float) else f"  {i}. [{score}] {title}")

        except Exception as e:
            logger.error(f"Vector DB search failed: {e}")
            logger.warning("Proceeding without similar fixes context...")
            similar_fixes = []

        # Step 2: Generate fix using LLM
        logger.info("\nGenerating data fix with LLM...")
        try:
            generated_fix = self.llm_agent.generate_data_fix(
                user_prompt=user_prompt,
                similar_fixes=similar_fixes
            )
            logger.info("✓ Data fix generated successfully")
        except Exception as e:
            logger.error(f"LLM generation failed: {e}")
            generated_fix = f"ERROR: Could not generate fix. Reason: {e}"

        return {
            "user_prompt": user_prompt,
            "similar_fixes": similar_fixes,
            "generated_fix": generated_fix
        }

    def interactive_mode(self):
        """
        Run the generator in interactive CLI mode.
        Accepts user prompts from stdin and outputs generated fixes.
        """
        print("\n" + "=" * 60)
        print("  Data Fix Generator - Interactive Mode")
        print("  Type 'quit' to exit, 'help' for usage")
        print("=" * 60 + "\n")

        while True:
            try:
                print("\nDescribe the data issue you need to fix:")
                print("(e.g., 'Duplicate customer records in CUSTOMER table with same email but different IDs')")
                user_input = input("\n> ").strip()

                if user_input.lower() in ["quit", "exit", "q"]:
                    print("Goodbye!")
                    break

                if user_input.lower() == "help":
                    self._print_help()
                    continue

                if not user_input:
                    print("Please enter a description of the data issue.")
                    continue

                # Optional: ask for top_k
                k_input = input(f"\nHow many similar fixes to retrieve? [default: {TOP_K_RESULTS}]: ").strip()
                top_k = int(k_input) if k_input.isdigit() else TOP_K_RESULTS

                result = self.generate(user_prompt=user_input, top_k=top_k)

                print("\n" + "=" * 60)
                print("SIMILAR HISTORICAL FIXES FOUND:")
                print("=" * 60)
                for i, fix in enumerate(result["similar_fixes"], 1):
                    payload = fix.get("payload", fix)
                    print(f"\n[{i}] {payload.get('title', 'Untitled')} ({payload.get('source', 'unknown')})")
                    if fix.get("score"):
                        print(f"    Similarity: {fix['score']:.3f}")

                print("\n" + "=" * 60)
                print("GENERATED DATA FIX:")
                print("=" * 60)
                print(result["generated_fix"])
                print("\n" + "=" * 60)

                # Optionally save the output
                save = input("\nSave this fix to a file? [y/N]: ").strip().lower()
                if save == "y":
                    filename = input("Filename [data_fix_output.sql]: ").strip() or "data_fix_output.sql"
                    with open(filename, "w") as f:
                        f.write(f"-- Generated Data Fix\n")
                        f.write(f"-- User Prompt: {user_input}\n\n")
                        f.write(result["generated_fix"])
                    print(f"Saved to: {filename}")

            except KeyboardInterrupt:
                print("\n\nInterrupted. Goodbye!")
                break
            except Exception as e:
                logger.error(f"Error during generation: {e}")
                print(f"\nError: {e}\nPlease try again.")

    def _print_help(self):
        print("""
Data Fix Generator Help
-----------------------
This tool generates data fix scripts based on:
  1. Your description of the data issue
  2. Similar historical fixes from the vector database (Jira/Confluence/Bitbucket)

Example prompts:
  - "Duplicate records in TRADE table where trade_id is the same but booking_date differs"
  - "Customer account balance is negative in ACCOUNT table for accounts that should be closed"
  - "Missing foreign key reference in ORDER_ITEMS for orders placed last Friday"
  - "Incorrectly set status flag in POSITION table for all APAC trades from 2024-03-01"

The tool will:
  1. Search the vector DB for similar past fixes
  2. Use those as examples for the LLM
  3. Generate a new fix script tailored to your issue
""")
