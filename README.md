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
| `requestAccess` | Grant an access profile — server-side policy: only `ACTIVE` users, no duplicates |
| `revokeAccess` | Remove an access profile the user currently holds |
| `listAuditEntries` | Audit trail of every grant and revocation |

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

Then try: *"Which users have pending offboarding?"*, *"What access does Bruno Tavares have?"*, or *"Grant Carla the finance read profile"* — and watch the policy engine refuse (she's suspended).

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

## Container

```bash
docker build -t iam-mcp-server .
docker run -p 8080:8080 iam-mcp-server
```

Multi-stage build: Gradle + JDK 21 compile the boot jar, runtime image is JRE-only and runs as a non-root user.

## Design notes

**Why tools instead of raw APIs.** An agent with `getUser`/`requestAccess` discovers a typed, documented surface and can't invent endpoints. Tool descriptions carry the policy ("fails for suspended users...") so the LLM plans within it before a call is ever made.

**Policy lives server-side, by design.** Tool descriptions help the agent *plan*; they don't *enforce* anything. The rules — only `ACTIVE` users receive access, assignments are unique, revocations require existing grants — are enforced in `IdentityService`, never trusted from the client.

**Two error vocabularies.** Bad input (unknown ids) throws `IllegalArgumentException`; policy denials (suspended user, duplicate, revoke of unheld profile) throw `IllegalStateException`. The agent gets a precise, actionable error either way — and the distinction reads naturally in an audit-adjacent codebase.

**Testable time.** The audit log stamps entries through an injected `java.time.Clock`, so tests pin `Clock.fixed(...)` and assert exact instants instead of sleeping or reaching for mocking libraries.

**Persistence is a swap, not a rewrite.** The store is in-memory seed data, but tools depend on the service contract, not the storage. Adding Postgres (JPA/R2DBC) changes the infrastructure layer only.

## Roadmap

- [x] **M1** — skeleton, read-only tools, CI
- [x] **M2** — write operations (request/revoke access) with policy validation + audit log
- [x] **M3** — container image, release automation, design notes
- [ ] **Next** — Postgres persistence, OpenTelemetry traces, streamable-HTTP auth
