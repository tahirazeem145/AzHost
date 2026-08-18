# AZHost — GitHub Security & Threat Model (Phase 7)

## Overview

Security is a foundational requirement of Phase 7 GitHub Integration in AZHost. Sensitive OAuth access tokens, external source archives, and API interactions are strictly controlled under a zero-trust model.

---

## Security Boundaries & Controls

### 1. OAuth CSRF State Protection

- **State Generation**: Generated using Java `SecureRandom` (32 cryptographically secure random bytes).
- **User Binding**: Every generated state token is bound to the authenticated AZHost user ID.
- **Expiration (TTL)**: States expire automatically after 5 minutes (300 seconds).
- **Single-Use Consumption**: States are purged immediately upon verification attempt to prevent state replay attacks.
- **Callback Validation**: Calls to `/api/github/callback` without valid, matching, non-expired states are immediately rejected with HTTP 401.

### 2. Authenticated Encryption at Rest (AES-256-GCM)

- **Algorithm**: AES-256 in Galois/Counter Mode (GCM) with 128-bit authentication tags (`AES/GCM/NoPadding`).
- **Random Initialization Vector**: A fresh 12-byte random IV is generated per encryption operation via `SecureRandom`.
- **Key Management**: Loaded from configuration `azhost.security.github-token-encryption-key`. Production keys must be 256-bit (64 hex characters or 32 base64 bytes).
- **Plaintext Safeguards**: Plaintext tokens are never persisted in the database, logged, included in DTO responses, or placed inside build container environment variables.

### 3. Repository Access Verification

- **Repository Selection by ID**: Repositories are selected by numeric ID (`githubRepositoryId`), never by untrusted arbitrary URLs (`https://evil.example.com/repo.git`).
- **Access Authorization**: The server verifies that the repository ID belongs to the authenticated user's GitHub connection prior to linking or fetching.
- **Branch & Commit SHA Resolution**: Commit SHAs are resolved server-side via official GitHub REST API calls, avoiding user-supplied SHA spoofing.

### 4. Archive Security & Extraction Limits

Downloaded GitHub repository zipball archives are treated as untrusted external input and subjected to strict extraction rules:

- **ZIP-Slip Prevention**: Canonical path resolution verifies every extracted file remains strictly contained within the target workspace directory: `resolvedPath.startsWith(targetWorkspaceDir)`.
- **Illegal Path Rejection**: Entries with relative traversal (`..`), absolute paths, Windows drive letters (`C:`), or UNC shares (`\\`) are immediately blocked.
- **File Count Limit**: Maximum 10,000 files per archive.
- **Uncompressed Size Limit**: Maximum 500 MB total uncompressed size.
- **Symlink Escape Protection**: Symlinks pointing outside the workspace directory are rejected.

### 5. Docker Build Engine Isolation

- **Zero Credentials in Containers**: Decrypted GitHub OAuth tokens NEVER enter Docker build containers or build workspace environments.
- **Host Execution Restriction**: No `git clone`, `Runtime.exec`, `ProcessBuilder`, or shell processes are invoked on the host machine. All source acquisition occurs over HTTPS API streams.
- **Existing Container Sandbox**: Phase 4 two-container build isolation, read-only root filesystems, resource limits, and network constraints remain completely unchanged.
