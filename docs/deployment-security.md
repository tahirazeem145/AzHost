# AZHost Phase 5: Deployment Security & Threat Model

This document outlines the security architecture protecting the AZHost platform during static artifact deployment and HTTP serving.

---

## 1. ZIP-Slip & Path Traversal Protections

1. **Extraction Validation**: `ZipArtifactReader` validates every entry: `resolvedPath.normalize().startsWith(targetWorkspaceDir)`.
2. **Path Traversal Rejection**: Entries with `..`, leading slashes (`/`, `\`), Windows drive letters (`C:`), or UNC paths (`\\`) are immediately rejected.
3. **Static File Serving Protection**: `StaticHostingController` validates that requested file paths resolve strictly inside `deployments/{deploymentId}` before serving.

---

## 2. Resource Caps & ZIP Bomb Prevention

- **Max Artifact ZIP Size**: `500 MB`
- **Max Extracted Total Size**: `1 GB`
- **Max File Count**: `20,000 files`
- **Max Single File Size**: `100 MB`

---

## 3. Forbidden Executable Extensions

The deployment engine rejects artifacts containing server-side executable files:
- `.jsp`, `.php`, `.py`, `.rb`, `.cgi`, `.exe`, `.sh`, `.bat`, `.cmd`, `.pl`, `.dll`, `.so`

Uploaded static assets are served purely as inert static data.

---

## 4. Symlink & Junction Containment

`LocalStaticFilePublisher` checks all symbolic links during publication. Any symlink pointing outside the publication source directory triggers a `SecurityException` and aborts the deployment.
