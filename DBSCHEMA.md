# Database Schema for Cursor for Minecraft

## Overview

This document outlines the simplified database schema for the Cursor for Minecraft project. The database will focus on storing conversation history between users and the LLM, along with build iterations associated with those conversations.

## Database Choice

**SQLite** is recommended for this project because:
- File-based database that requires no separate server process
- Built-in JSON support (available since version 3.38.0)
- Zero configuration needed
- Perfect for single-server deployments
- Easy distribution with the plugin
- Sufficient for the expected data volume and complexity

## Schema Design

### 1. Conversations Table
Stores conversation threads between users and the LLM during the build creation process

```sql
CREATE TABLE conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_uuid TEXT NOT NULL, -- Minecraft player UUID
    user_username TEXT NOT NULL, -- Minecraft username
    title TEXT, -- Auto-generated title based on first user message
    status TEXT DEFAULT 'active' CHECK(status IN ('active', 'completed', 'cancelled')),
    current_mode TEXT DEFAULT 'PLANNING' CHECK(current_mode IN ('PLANNING', 'BUILDING')), -- New mode tracking
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT -- Additional conversation metadata (workflow used, etc.) stored as JSON
);
```

### 2. Builds Table
Stores build iterations with their JSON schema data, linked to conversations

```sql
CREATE TABLE builds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    iteration_number INTEGER NOT NULL DEFAULT 1,
    build_name TEXT,
    prompt TEXT, -- User prompt for this iteration
    schema_data TEXT, -- Generated VoxelJS schema or build data stored as JSON
    image_url TEXT, -- URL of reference image/video if provided
    status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'processing', 'completed', 'failed')),
    block_count INTEGER, -- Number of blocks in the schema
    dimensions TEXT, -- Dimensions of the build (width, height, depth) stored as JSON
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    UNIQUE(conversation_id, iteration_number)
);
```

-- Create indexes for better query performance
CREATE INDEX idx_conversations_user_created ON conversations(user_uuid, created_at);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_builds_conversation_iteration ON builds(conversation_id, iteration_number);
CREATE INDEX idx_builds_status ON builds(status);
```

## Data Flow

1. User initiates a build via `/aibuild create` command
2. Plugin creates a record in the `conversations` table
3. User and LLM exchange messages stored as part of the conversation
4. When a build schema is generated, it's stored in the `builds` table with a link to the conversation
5. Multiple build iterations can be stored for the same conversation
6. Completed conversations remain in the database for retrieval via `/aibuild list`

## Indexing Strategy

- Primary indexes created automatically by PRIMARY KEY constraints
- Index on user UUID and creation time for efficient conversation retrieval
- Index on conversation ID and iteration number for build ordering
- Index on status fields for filtering active/completed builds
- SQLite automatically manages indexes efficiently

## Alternative Approaches

### Option 1: Plugin-Only with SQLite (Recommended)
- Store all data in SQLite database embedded in the plugin
- File-based storage with no separate server required
- Full SQL capabilities with JSON support
- Perfect integration with Java plugin
- Recommended approach for this project

### Option 2: Flat File Storage
- Store data in JSON or YAML files
- Simple implementation but limited querying capabilities
- Drawback: No ACID transactions, potential concurrency issues