# AI-SERVICE

Standalone Python/FastAPI service for the RAG pipeline. Right now it just exposes a dummy POST /query endpoint (boilerplate JSON response), and defines the Postgres/pgvector schema for storing document chunks and their embeddings. Real retrieval logic isn't implemented yet.

## 1. Run the FastAPI service

### Prerequisites

- Python 3.10+ available on your PATH.

### Setup

Run in: PowerShell, from the ai-service/ folder.

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### Run

Run in: PowerShell (the same one, still activated, still inside ai-service/).

```powershell
uvicorn app.main:app --reload --port 8000
```

Keep this window open. The --reload flag restarts the server automatically as you edit code.

### Test it

Run in: a second PowerShell window (the first one is busy running the server).

```powershell
Invoke-RestMethod -Uri http://localhost:8000/query -Method Post -ContentType "application/json" -Body '{"query":"test"}'
```

Or in Bruno: method POST, URL http://localhost:8000/query, JSON body {"query": "test"}. Expected response:

```json
{
  "query": "test",
  "answer": "This is a placeholder response. The RAG pipeline is not implemented yet.",
  "sources": []
}
```

## 2. Database setup (Postgres + pgvector)

This service shares one Postgres database (genrag) with api-gateway. api-gateway owns users, chats, messages and documents. This service owns document_chunks (chunked document content + embeddings), which has a foreign key into documents. Both schemas are plain .sql scripts, applied manually - there's no migration tool wired up on either side yet.

### 2.1 Confirm Postgres is running

Run in: PowerShell

```powershell
Get-Service -Name postgresql*
```

### 2.2 Create the role and database

Run in: pgAdmin (GUI, connect to your existing Postgres 18 server in the left tree first).

api-gateway's application.properties expects a role/database both named genrag, password genrag by default (overridable via DB_USER/DB_PASSWORD env vars).

- Login/Group Roles, right-click, Create, Login/Group Role. Name it genrag, password genrag, and set "Can login?" to Yes.
- Databases, right-click, Create, Database. Name it genrag, owner genrag.

### 2.3 Create the base tables (users, chats, messages, documents)

Run in: pgAdmin Query Tool. Select the genrag database in the tree first, then Tools > Query Tool, open this file, and execute (F5):

```
api-gateway/src/main/java/com/genrag/config/db/schema_init.sql
```

Verify: users, chats, messages and documents should appear under genrag > Schemas > public > Tables (right-click Tables, Refresh).

### 2.4 Install the pgvector extension

pgvector isn't bundled with Postgres, it has to be built/installed on the Postgres server itself. Windows Stack Builder doesn't currently offer it for Postgres 18, so it needs to be built from source.

1. Run in: Visual Studio Installer (Start Menu, "Visual Studio Installer"). Install Visual Studio Build Tools with the "Desktop development with C++" workload - click Modify if you already have a VS install but not this workload.
2. Run in: "x64 Native Tools Command Prompt for VS 2022" (Start Menu, search for it by name; required because it preloads the MSVC compiler environment. A regular PowerShell/cmd window will not work here).
3. In that same Native Tools Command Prompt, build and install pgvector. Use the latest release tag - v0.8.0 fails to compile against Postgres 18 because of a vacuum_delay_point() signature change. v0.8.5 is current as of writing and includes a security fix from v0.8.2.

   ```cmd
   set "PGROOT=C:\Program Files\PostgreSQL\18"
   cd %USERPROFILE%\Desktop
   git clone --branch v0.8.5 https://github.com/pgvector/pgvector.git
   cd pgvector
   nmake /F Makefile.win
   nmake /F Makefile.win install
   ```

   If install fails with a permissions error: close this window, reopen "x64 Native Tools Command Prompt for VS 2022" as Administrator (right-click it, Run as administrator), then re-run the cd pgvector step plus both nmake commands.

4. Run in: Windows Services app. Win+R, type services.msc, Enter, find postgresql-x64-18, Restart.
5. Run in: pgAdmin Query Tool (on the genrag database), confirm it's available:

   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

### 2.5 Create the document_chunks table

Run in: pgAdmin Query Tool. Select the genrag database, Tools > Query Tool, open this file, and execute (F5):

```
ai-service/db/schema_init.sql
```

This creates document_chunks (with a foreign key into documents, so step 2.3 has to be done first) and the idx_chunks_embedding ivfflat index.

Verify: genrag > Schemas > public > Tables should now list document_chunks (right-click Tables, Refresh). Expand it, Indexes, should list idx_chunks_embedding.

Note: the embedding dimension (VECTOR(768)) is a placeholder until the embedding model is finalized. It'll need to change if a different model gets picked later.
