# MiniRedis

A Redis-compatible in-memory key-value store written from scratch in Java 21, plus a Spring Boot
starter so any Spring Boot app can use it as a cache.

> Work in progress: currently at **Phase 0** (project skeleton + CI).

## Build

Requires JDK 21+ and Maven.

```bash
mvn verify
```

## Modules

| Module | What it is |
|---|---|
| `miniredis-server` | TCP server speaking RESP2. Zero runtime dependencies. |
