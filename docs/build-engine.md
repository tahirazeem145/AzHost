# AZHost Phase 4: Source Acquisition & Build Engine Specification

This document details the technical architecture and operation of the **Phase 4 Build Engine** in AZHost.

---

## 1. Overview & Objectives

Phase 4 introduces controlled project source acquisition, workspace allocation, and isolated Two-Container Docker build execution.

- **Primary Goal**: Turn project source code into compiled, packaged **Build Artifacts** (`artifacts/{artifactId}.zip`).
- **Scope Restriction**: Phase 4 DOES NOT perform public deployment or container hosting. Deployment belongs to Phase 5.

---

## 2. Two-Container Isolated Build Pipeline

```text
       Project (Local / ZIP Source)
                    │
                    ▼
     Workspace Allocation (workspaces/build-{UUID})
                    │
                    ▼
    [ Container A: Dependency Install Stage ]
       - Official Node Docker image (e.g. node:20-alpine)
       - Exec: ["npm", "ci", "--ignore-scripts"] (or pnpm/yarn/bun)
       - Restricted network access to registry
                    │
                    ▼
    [ Container B: Build Execution Stage ]
       - Official Node Docker image (node:20-alpine)
       - Exec: ["npm", "run", "build"]
       - 100% Offline (--network none)
                    │
                    ▼
     Packaging Artifact (artifacts/{artifactId}.zip)
                    │
                    ▼
       Workspace Cleanup & Status Update
```

---

## 3. Security Limits & Container Hardening

- **Non-Root User**: Runs as `--user 1000:1000` (unprivileged Node user).
- **Dropped Capabilities**: `--cap-drop=ALL --security-opt=no-new-privileges`.
- **Read-Only Root Filesystem**: `--read-only` with `/tmp` tmpfs (512 MB).
- **Resource Limits**:
  - CPU: `2.0 cores`
  - Memory: `2048 MB`
  - Timeout: `600 seconds` (10 minutes)
  - Log Max: `10 MB`
  - Artifact Max: `500 MB`
- **Whitelisted Base Images**: `node:18-alpine`, `node:20-alpine`, `node:22-alpine` (hardcoded mapping). Arbitrary image names are strictly rejected.

---

## 4. API Endpoints

- `POST /api/projects/{id}/builds`: Triggers async build execution (`202 Accepted`). Returns 409 Conflict if source is unavailable or build already in progress.
- `GET /api/projects/{id}/builds`: Returns list of project build records (`200 OK`).
- `GET /api/projects/{id}/builds/{buildId}`: Returns build details (`200 OK`).
- `GET /api/projects/{id}/builds/{buildId}/logs`: Returns streaming/buffered logs (`200 OK`).
- `POST /api/projects/{id}/builds/{buildId}/cancel`: Cancels an active build (`200 OK`).
