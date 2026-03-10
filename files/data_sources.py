"""
Data Source Fetchers
Fetches historical data fixes from: Jira, Confluence, Bitbucket, Local Machine
Per the architecture diagram - all sources feed into the Python pipeline.
"""

import os
import json
import logging
from pathlib import Path
from typing import Optional
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


@dataclass
class DataFixRecord:
    """Represents a single historical data fix record"""
    id: str
    source: str          # "jira", "confluence", "bitbucket", "local"
    title: str
    content: str         # The actual fix content / SQL / description
    metadata: dict = field(default_factory=dict)

    def to_text_for_embedding(self) -> str:
        """Combine fields into a single string for embedding"""
        return f"Title: {self.title}\nSource: {self.source}\nContent: {self.content}"

    def to_payload(self) -> dict:
        """Convert to Qdrant point payload"""
        return {
            "id": self.id,
            "source": self.source,
            "title": self.title,
            "content": self.content,
            **self.metadata
        }


class JiraFetcher:
    """
    Fetches data fix tickets from Jira
    Looks for tickets labeled 'data-fix' or in a specific project
    """

    def __init__(self, base_url: str, email: str, api_token: str):
        import base64
        credentials = base64.b64encode(f"{email}:{api_token}".encode()).decode()
        self.base_url = base_url.rstrip("/")
        self.headers = {
            "Authorization": f"Basic {credentials}",
            "Content-Type": "application/json"
        }

    def fetch_data_fixes(self, jql: str = 'labels = "data-fix" ORDER BY created DESC', max_results: int = 100) -> list[DataFixRecord]:
        """
        Fetch Jira tickets matching JQL query
        
        Args:
            jql: Jira Query Language filter
            max_results: Max tickets to fetch
        """
        import requests
        url = f"{self.base_url}/rest/api/3/search"
        params = {
            "jql": jql,
            "maxResults": max_results,
            "fields": "summary,description,comment,status,created,labels,issuetype"
        }

        logger.info(f"Fetching Jira tickets with JQL: {jql}")
        response = requests.get(url, headers=self.headers, params=params)
        response.raise_for_status()
        issues = response.json().get("issues", [])

        records = []
        for issue in issues:
            fields = issue.get("fields", {})
            description = self._extract_description(fields.get("description"))
            
            # Also grab comments which often contain the actual SQL fix
            comments = self._extract_comments(fields.get("comment", {}).get("comments", []))

            content = f"{description}\n\nComments:\n{comments}".strip()

            records.append(DataFixRecord(
                id=f"jira_{issue['key']}",
                source="jira",
                title=fields.get("summary", "No title"),
                content=content,
                metadata={
                    "jira_key": issue["key"],
                    "status": fields.get("status", {}).get("name", ""),
                    "created": fields.get("created", ""),
                    "labels": fields.get("labels", []),
                    "url": f"{self.base_url}/browse/{issue['key']}"
                }
            ))

        logger.info(f"Fetched {len(records)} data fix records from Jira")
        return records

    def _extract_description(self, description) -> str:
        """Extract plain text from Jira's Atlassian Document Format (ADF)"""
        if not description:
            return ""
        if isinstance(description, str):
            return description
        # Handle ADF format
        texts = []
        def extract_text(node):
            if isinstance(node, dict):
                if node.get("type") == "text":
                    texts.append(node.get("text", ""))
                for child in node.get("content", []):
                    extract_text(child)
        extract_text(description)
        return " ".join(texts)

    def _extract_comments(self, comments: list) -> str:
        """Extract comment text"""
        result = []
        for comment in comments:
            body = self._extract_description(comment.get("body"))
            if body:
                author = comment.get("author", {}).get("displayName", "Unknown")
                result.append(f"[{author}]: {body}")
        return "\n".join(result)


class ConfluenceFetcher:
    """
    Fetches data fix documentation pages from Confluence
    """

    def __init__(self, base_url: str, email: str, api_token: str):
        import base64
        credentials = base64.b64encode(f"{email}:{api_token}".encode()).decode()
        self.base_url = base_url.rstrip("/")
        self.headers = {
            "Authorization": f"Basic {credentials}",
            "Content-Type": "application/json"
        }

    def fetch_data_fixes(self, space_key: str, label: str = "data-fix", limit: int = 50) -> list[DataFixRecord]:
        """
        Fetch Confluence pages tagged with a specific label in a space
        
        Args:
            space_key: Confluence space key (e.g. "TEAM", "DATA")
            label: Page label to filter by
            limit: Max pages to fetch
        """
        import requests
        url = f"{self.base_url}/wiki/rest/api/content"
        params = {
            "type": "page",
            "spaceKey": space_key,
            "label": label,
            "limit": limit,
            "expand": "body.storage,metadata.labels"
        }

        logger.info(f"Fetching Confluence pages from space {space_key} with label '{label}'")
        response = requests.get(url, headers=self.headers, params=params)
        response.raise_for_status()
        pages = response.json().get("results", [])

        records = []
        for page in pages:
            # Strip HTML from body
            body_html = page.get("body", {}).get("storage", {}).get("value", "")
            body_text = self._strip_html(body_html)

            records.append(DataFixRecord(
                id=f"confluence_{page['id']}",
                source="confluence",
                title=page.get("title", "Untitled"),
                content=body_text,
                metadata={
                    "page_id": page["id"],
                    "space": space_key,
                    "url": f"{self.base_url}/wiki{page.get('_links', {}).get('webui', '')}"
                }
            ))

        logger.info(f"Fetched {len(records)} pages from Confluence")
        return records

    def _strip_html(self, html: str) -> str:
        """Remove HTML tags from Confluence storage format"""
        import re
        clean = re.sub(r'<[^>]+>', ' ', html)
        clean = re.sub(r'\s+', ' ', clean).strip()
        return clean


class BitbucketFetcher:
    """
    Fetches data fix scripts from Bitbucket repositories
    """

    def __init__(self, base_url: str, token: str):
        self.base_url = base_url.rstrip("/")
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

    def fetch_data_fixes(self, workspace: str, repo_slug: str, path: str = "data-fixes/", branch: str = "main") -> list[DataFixRecord]:
        """
        Fetch SQL/script files from a Bitbucket repository path
        
        Args:
            workspace: Bitbucket workspace name
            repo_slug: Repository slug
            path: Path within repo to look for fix scripts
            branch: Branch to read from
        """
        import requests
        url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/src/{branch}/{path}"
        logger.info(f"Fetching files from Bitbucket: {workspace}/{repo_slug}/{path}")

        response = requests.get(url, headers=self.headers)
        if response.status_code == 404:
            logger.warning(f"Path {path} not found in {repo_slug}")
            return []
        response.raise_for_status()

        files = response.json().get("values", [])
        records = []

        for file_entry in files:
            if file_entry.get("type") != "commit_file":
                continue
            file_path = file_entry.get("path", "")
            if not any(file_path.endswith(ext) for ext in [".sql", ".py", ".sh", ".txt"]):
                continue

            # Fetch file content
            content_url = f"{self.base_url}/repositories/{workspace}/{repo_slug}/src/{branch}/{file_path}"
            content_response = requests.get(content_url, headers=self.headers)
            if content_response.status_code != 200:
                continue

            file_content = content_response.text
            file_name = Path(file_path).name

            records.append(DataFixRecord(
                id=f"bitbucket_{workspace}_{repo_slug}_{file_name}",
                source="bitbucket",
                title=file_name,
                content=file_content,
                metadata={
                    "repo": f"{workspace}/{repo_slug}",
                    "file_path": file_path,
                    "branch": branch
                }
            ))

        logger.info(f"Fetched {len(records)} fix scripts from Bitbucket")
        return records


class LocalMachineFetcher:
    """
    Fetches data fix files stored locally
    Per meeting: "api for embedding can be found in ML Core or can be picked from local file for POC"
    """

    def __init__(self, local_data_path: str = "./local_data_fixes"):
        self.base_path = Path(local_data_path)

    def fetch_data_fixes(self, extensions: list[str] = None) -> list[DataFixRecord]:
        """
        Read all fix files from local directory
        
        Args:
            extensions: File extensions to include (default: sql, py, sh, txt, json)
        """
        if extensions is None:
            extensions = [".sql", ".py", ".sh", ".txt", ".json", ".md"]

        if not self.base_path.exists():
            logger.warning(f"Local data path {self.base_path} does not exist")
            return []

        records = []
        for file_path in self.base_path.rglob("*"):
            if file_path.suffix not in extensions:
                continue
            try:
                content = file_path.read_text(encoding="utf-8")
                records.append(DataFixRecord(
                    id=f"local_{file_path.stem}",
                    source="local",
                    title=file_path.name,
                    content=content,
                    metadata={
                        "file_path": str(file_path),
                        "size_bytes": file_path.stat().st_size
                    }
                ))
            except Exception as e:
                logger.warning(f"Could not read {file_path}: {e}")

        logger.info(f"Fetched {len(records)} fix files from local machine")
        return records


def fetch_all_sources(
    jira_config: Optional[dict] = None,
    confluence_config: Optional[dict] = None,
    bitbucket_config: Optional[dict] = None,
    local_path: Optional[str] = None
) -> list[DataFixRecord]:
    """
    Aggregate data fix records from all configured sources.
    
    Args:
        jira_config: {"base_url", "email", "api_token", "jql"} or None to skip
        confluence_config: {"base_url", "email", "api_token", "space_key"} or None to skip
        bitbucket_config: {"base_url", "token", "workspace", "repo_slug", "path"} or None to skip
        local_path: Path to local data fix files or None to skip
        
    Returns:
        Combined list of DataFixRecord from all sources
    """
    all_records = []

    if jira_config:
        try:
            fetcher = JiraFetcher(
                jira_config["base_url"],
                jira_config["email"],
                jira_config["api_token"]
            )
            records = fetcher.fetch_data_fixes(jira_config.get("jql", 'labels = "data-fix"'))
            all_records.extend(records)
        except Exception as e:
            logger.error(f"Jira fetch failed: {e}")

    if confluence_config:
        try:
            fetcher = ConfluenceFetcher(
                confluence_config["base_url"],
                confluence_config["email"],
                confluence_config["api_token"]
            )
            records = fetcher.fetch_data_fixes(confluence_config["space_key"])
            all_records.extend(records)
        except Exception as e:
            logger.error(f"Confluence fetch failed: {e}")

    if bitbucket_config:
        try:
            fetcher = BitbucketFetcher(
                bitbucket_config["base_url"],
                bitbucket_config["token"]
            )
            records = fetcher.fetch_data_fixes(
                bitbucket_config["workspace"],
                bitbucket_config["repo_slug"],
                bitbucket_config.get("path", "data-fixes/")
            )
            all_records.extend(records)
        except Exception as e:
            logger.error(f"Bitbucket fetch failed: {e}")

    if local_path:
        try:
            fetcher = LocalMachineFetcher(local_path)
            records = fetcher.fetch_data_fixes()
            all_records.extend(records)
        except Exception as e:
            logger.error(f"Local fetch failed: {e}")

    logger.info(f"Total records fetched from all sources: {len(all_records)}")
    return all_records
