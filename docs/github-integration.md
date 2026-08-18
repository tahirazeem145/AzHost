# AZHost — GitHub Source Integration Architecture (Phase 7)

## Overview

Phase 7 introduces GitHub Source Integration into AZHost, enabling users to securely link GitHub repositories to AZHost projects. GitHub acts as a source provider feeding directly into AZHost's existing Phase 3 analysis, Phase 4 isolated build engine, Phase 5 deployment pipeline, and Phase 6 production routing layer.

```
GitHub OAuth Flow
       │
       ▼
GitHub Connection (AES-256-GCM Encrypted Token at Rest)
       │
       ▼
Repository & Branch Selection (Commit SHA Resolution)
       │
       ▼
Project GitHub Link
       │
       ▼
GitHubSourceProvider (HTTPS Archive API Download)
       │
       ▼
Temporary Build Workspace (ZIP-Slip & Size Limit Safeguards)
       │
       ▼
Phase 3 Analysis → Phase 4 Build Engine → Phase 5 Deployment → Phase 6 Routing
```

---

## Architectural Principles

1. **GitHub is a Source Provider**: GitHub is not a separate build engine. Source acquired from GitHub feeds directly into AZHost's standard build and deployment pipeline.
2. **No Host `git` Execution**: Repositories are fetched strictly via HTTPS GitHub API archive endpoints (`/repositories/{id}/zipball/{commit}`). No `git clone`, `Runtime.exec`, or shell processes are executed on the host.
3. **Commit SHA Pinning**: Selected branches are resolved to an immutable 40-character Git commit SHA to ensure build reproducibility.
4. **Token Isolation**: OAuth tokens are encrypted at rest using AES-256-GCM and never exposed to the frontend, logged, or placed inside build container environment variables.

---

## API Endpoints

### GitHub Connection Management

- `GET /api/github/connect`: Generates a GitHub OAuth authorization URL with a cryptographically secure CSRF `state` parameter.
- `GET /api/github/callback?code=...&state=...`: Validates the OAuth state, exchanges the authorization code for an access token, fetches the user's profile, encrypts the access token at rest, and redirects to the frontend.
- `GET /api/github/connection`: Returns public connection status (`connected`, `githubUsername`, `avatarUrl`, `connectedAt`).
- `DELETE /api/github/connection`: Revokes and deletes the stored GitHub connection for the user.

### Repository & Branch Browsing

- `GET /api/github/repositories`: Returns a list of repositories accessible to the authenticated GitHub account.
- `GET /api/github/repositories/{repoId}/branches`: Returns accessible branches for the specified repository ID.

### Project Linking

- `POST /api/projects/{projectId}/github`: Links an AZHost project to a GitHub repository ID and branch, resolves the commit SHA, and sets source type to `GITHUB`.
- `DELETE /api/projects/{projectId}/github`: Removes the GitHub link, reverting source type to `LOCAL` while preserving existing builds and deployments.

---

## Source Provider Pipeline

```java
public class GitHubSourceProvider implements SourceProvider {
    @Override
    public boolean supports(Project project) {
        return project.getSourceType() == ProjectSourceType.GITHUB;
    }

    @Override
    public SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException {
        // 1. Validate repository link & commit SHA
        // 2. Fetch & decrypt OAuth token
        // 3. Download zipball from GitHub HTTPS API
        // 4. Safely extract files, stripping top-level directory wrapper
        // 5. Enforce ZIP-Slip, file count, and size limit checks
        // 6. Return SourceAcquisitionResult
    }
}
```

---

## Configuration Properties

```yaml
azhost:
  github:
    client-id: ${AZHOST_GITHUB_CLIENT_ID:${GITHUB_CLIENT_ID:dummy-github-client-id}}
    client-secret: ${AZHOST_GITHUB_CLIENT_SECRET:${GITHUB_CLIENT_SECRET:dummy-github-client-secret}}
    redirect-uri: ${AZHOST_GITHUB_REDIRECT_URI:${GITHUB_REDIRECT_URI:http://localhost:8080/api/github/callback}}
    api-base-url: ${AZHOST_GITHUB_API_BASE_URL:https://api.github.com}
    oauth-url: ${AZHOST_GITHUB_OAUTH_URL:https://github.com/login/oauth}
  security:
    github-token-encryption-key: ${AZHOST_GITHUB_TOKEN_ENCRYPTION_KEY:${GITHUB_TOKEN_ENCRYPTION_KEY:}}
```
