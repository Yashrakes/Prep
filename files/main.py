"""
Data Fix Generator - Main Entry Point

Usage:
  # Ingest historical data fixes into vector DB:
  python main.py ingest [--local ./local_data_fixes] [--force-recreate]

  # Generate a data fix interactively:
  python main.py generate

  # Generate a data fix from command line:
  python main.py generate --prompt "Duplicate records in TRADE table"

  # Health check:
  python main.py health
"""

import os
import sys
import argparse
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("data_fix_generator.log")
    ]
)
logger = logging.getLogger(__name__)


def get_configs_from_env() -> dict:
    """
    Load source configs from environment variables.
    Set these in your shell or .env file before running.
    """
    configs = {}

    # Jira config
    if os.getenv("JIRA_BASE_URL") and os.getenv("JIRA_USER_EMAIL") and os.getenv("JIRA_API_TOKEN"):
        configs["jira"] = {
            "base_url": os.getenv("JIRA_BASE_URL"),
            "email": os.getenv("JIRA_USER_EMAIL"),
            "api_token": os.getenv("JIRA_API_TOKEN"),
            "jql": os.getenv("JIRA_JQL", 'labels = "data-fix" ORDER BY created DESC')
        }
        logger.info("✓ Jira config loaded from environment")
    else:
        logger.info("- Jira config not set (skip or set JIRA_BASE_URL, JIRA_USER_EMAIL, JIRA_API_TOKEN)")

    # Confluence config
    if os.getenv("CONFLUENCE_BASE_URL") and os.getenv("CONFLUENCE_USER_EMAIL") and os.getenv("CONFLUENCE_API_TOKEN"):
        configs["confluence"] = {
            "base_url": os.getenv("CONFLUENCE_BASE_URL"),
            "email": os.getenv("CONFLUENCE_USER_EMAIL"),
            "api_token": os.getenv("CONFLUENCE_API_TOKEN"),
            "space_key": os.getenv("CONFLUENCE_SPACE_KEY", "DATA")
        }
        logger.info("✓ Confluence config loaded from environment")
    else:
        logger.info("- Confluence config not set (skip or set CONFLUENCE_BASE_URL, etc.)")

    # Bitbucket config
    if os.getenv("BITBUCKET_TOKEN") and os.getenv("BITBUCKET_WORKSPACE") and os.getenv("BITBUCKET_REPO_SLUG"):
        configs["bitbucket"] = {
            "base_url": os.getenv("BITBUCKET_BASE_URL", "https://api.bitbucket.org/2.0"),
            "token": os.getenv("BITBUCKET_TOKEN"),
            "workspace": os.getenv("BITBUCKET_WORKSPACE"),
            "repo_slug": os.getenv("BITBUCKET_REPO_SLUG"),
            "path": os.getenv("BITBUCKET_DATA_FIX_PATH", "data-fixes/")
        }
        logger.info("✓ Bitbucket config loaded from environment")
    else:
        logger.info("- Bitbucket config not set (skip or set BITBUCKET_TOKEN, BITBUCKET_WORKSPACE, BITBUCKET_REPO_SLUG)")

    return configs


def cmd_health(args):
    """Check health of all ML-Core APIs"""
    from mlcore_client import EmbeddingsAPIClient, VectorDBAPIClient

    print("\n--- Health Check ---")
    api_key = os.getenv("MLCORE_API_KEY")

    embed_client = EmbeddingsAPIClient(api_key=api_key)
    try:
        result = embed_client.health_check()
        print(f"✓ Embeddings API: {result}")
    except Exception as e:
        print(f"✗ Embeddings API: {e}")

    vectordb_client = VectorDBAPIClient(api_key=api_key)
    try:
        result = vectordb_client.health_check()
        print(f"✓ Vector DB API: {result}")
    except Exception as e:
        print(f"✗ Vector DB API: {e}")


def cmd_ingest(args):
    """Run the ingestion pipeline"""
    from ingestion_pipeline import DataFixIngestionPipeline

    api_key = os.getenv("MLCORE_API_KEY")
    pipeline = DataFixIngestionPipeline(api_key=api_key)

    source_configs = get_configs_from_env()
    local_path = args.local if args.local else os.getenv("LOCAL_DATA_PATH")

    summary = pipeline.run(
        jira_config=source_configs.get("jira"),
        confluence_config=source_configs.get("confluence"),
        bitbucket_config=source_configs.get("bitbucket"),
        local_path=local_path,
        force_recreate=args.force_recreate,
        mode=args.mode
    )

    print(f"\n✓ Ingestion complete: {summary}")


def cmd_generate(args):
    """Run the data fix generator"""
    from data_fix_generator import DataFixGenerator

    api_key = os.getenv("MLCORE_API_KEY")
    llm_api_key = os.getenv("LLM_API_KEY")
    llm_mode = "api" if llm_api_key else "local"

    generator = DataFixGenerator(
        api_key=api_key,
        llm_mode=llm_mode,
        llm_api_key=llm_api_key
    )

    if args.prompt:
        # Single prompt mode
        result = generator.generate(user_prompt=args.prompt, top_k=args.top_k)

        print("\n" + "=" * 60)
        print("SIMILAR HISTORICAL FIXES:")
        for i, fix in enumerate(result["similar_fixes"], 1):
            payload = fix.get("payload", fix)
            print(f"  {i}. {payload.get('title', 'Untitled')} [{payload.get('source', '')}]")

        print("\n" + "=" * 60)
        print("GENERATED DATA FIX:")
        print("=" * 60)
        print(result["generated_fix"])

        if args.output:
            with open(args.output, "w") as f:
                f.write(result["generated_fix"])
            print(f"\nSaved to: {args.output}")
    else:
        # Interactive mode
        generator.interactive_mode()


def main():
    parser = argparse.ArgumentParser(
        description="Data Fix Generator - RAG-based data fix script generator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Environment Variables:
  MLCORE_API_KEY          ML-Core API authentication key
  LLM_API_KEY             LLM API key (leave unset to use local Ollama)
  JIRA_BASE_URL           Jira instance URL
  JIRA_USER_EMAIL         Jira user email
  JIRA_API_TOKEN          Jira API token
  CONFLUENCE_BASE_URL     Confluence instance URL
  CONFLUENCE_USER_EMAIL   Confluence user email
  CONFLUENCE_API_TOKEN    Confluence API token
  CONFLUENCE_SPACE_KEY    Confluence space key (default: DATA)
  BITBUCKET_TOKEN         Bitbucket access token
  BITBUCKET_WORKSPACE     Bitbucket workspace name
  BITBUCKET_REPO_SLUG     Bitbucket repository slug
  LOCAL_DATA_PATH         Path to local data fix files

Examples:
  python main.py health
  python main.py ingest --local ./local_data_fixes
  python main.py ingest --local ./fixes --force-recreate
  python main.py generate
  python main.py generate --prompt "Duplicate trades in TRADE_BOOKING table"
  python main.py generate --prompt "Fix null values in POSITION.book_id" --output fix.sql
"""
    )

    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    # Health command
    subparsers.add_parser("health", help="Check API health")

    # Ingest command
    ingest_parser = subparsers.add_parser("ingest", help="Ingest historical data fixes into vector DB")
    ingest_parser.add_argument("--local", type=str, help="Path to local data fix files")
    ingest_parser.add_argument("--force-recreate", action="store_true",
                               help="Delete and recreate the Qdrant collection")
    ingest_parser.add_argument("--mode", choices=["add", "insert"], default="add",
                               help="Storage mode: 'add' appends, 'insert' overwrites (default: add)")

    # Generate command
    gen_parser = subparsers.add_parser("generate", help="Generate a data fix")
    gen_parser.add_argument("--prompt", type=str,
                            help="Data issue description. If not provided, runs in interactive mode.")
    gen_parser.add_argument("--top-k", type=int, default=5,
                            help="Number of similar fixes to retrieve from vector DB (default: 5)")
    gen_parser.add_argument("--output", type=str,
                            help="Output file to save generated fix (optional)")

    args = parser.parse_args()

    if args.command == "health":
        cmd_health(args)
    elif args.command == "ingest":
        cmd_ingest(args)
    elif args.command == "generate":
        cmd_generate(args)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
