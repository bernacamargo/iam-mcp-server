# iam-mcp-server

[![CI](https://github.com/bernacamargo/iam-mcp-server/actions/workflows/ci.yml/badge.svg)](https://github.com/bernacamargo/iam-mcp-server/actions/workflows/ci.yml)

A **Model Context Protocol (MCP) server** that exposes identity & access governance (IAM) tools to AI agents — built with **Kotlin**, **Spring Boot 4**, and **Spring AI 2**.

The use case mirrors production IAM work: instead of an AI agent calling raw admin APIs (or hallucinating them), it discovers typed, documented tools for looking up users, their access profiles, and entitlements — with the business rules living server-side.

> Portfolio project. The identity store is in-memory seed data; the value is the production-grade MCP integration, not persistence.

## Tools exposed

| Tool | Description |
|---|---|
| `listUsers` | List workforce users, optionally filtered by status (`ACTIVE`, `SUSPENDED`, `PENDING_OFFBOARDING`) |
| `getUser` | One user by id, including assigned access profiles |
| `listAccessProfiles` | All access profiles with their entitlements |

## Stack

- **Kotlin** + Spring Boot 4.1 (JVM 21, Gradle Kotlin DSL)
- **Spring AI 2.0** — MCP server starter (`spring-ai-starter-mcp-server-webmvc`), tools declared with `@Tool` / `@ToolParam`
- JUnit 5 + AssertJ

## Run it

```bash
./gradlew bootRun
```

The MCP endpoint is served over streamable HTTP at `http://localhost:8080/mcp`.

Point any MCP client at it, e.g. Claude Code:

```json
{
  "mcpServers": {
    "iam": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

Then try: *"Which users have pending offboarding?"* or *"What access does Bruno Tavares have?"*

## Architecture

```
MCP client (Claude, IDE agent, ...)
        │  streamable HTTP (/mcp)
        ▼
┌──────────────────────────────┐
│  Spring AI MCP server layer  │   tool discovery: @Tool annotations
│  ──────────────────────────  │
│  IdentityTools               │   typed tool surface
│  ──────────────────────────  │
│  IdentityService             │   business rules + in-memory store
│  ──────────────────────────  │
│  domain (User, AccessProfile)│   pure Kotlin, no framework imports
└──────────────────────────────┘
```

## Roadmap

- [x] **M1** — skeleton, three read-only tools, CI
- [ ] **M2** — write operations (request/revoke access) with validation + audit log
- [ ] **M3** — container image, releases, deep-dive documentation
