# AZHost Security Hardening Architecture

This document outlines the security controls, headers, and container isolation mechanisms used in AZHost.

## HTTP Security Headers
The following HTTP security headers are enforced in production via Spring Security:
* `X-Content-Type-Options: nosniff`: Prevents MIME-sniffing vulnerabilities.
* `X-Frame-Options: DENY`: Prevents Clickjacking attacks by disabling framing of endpoints.
* `Referrer-Policy: strict-origin-when-cross-origin`: Controls referrer data leaks.
* `Strict-Transport-Security: max-age=31536000; includeSubDomains`: Enforces HTTPS.
* `Content-Security-Policy`: Standard restriction policy limiting browser execution to local site assets and preventing data injection:
  `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; object-src 'none';`

## CORS Configuration
* Wildcard (`*`) is completely disabled in the `prod` profile.
* Allowed origins must be explicitly listed in the `CORS_ALLOWED_ORIGINS` environment variable.
* Credential support is enabled only for matching explicit domains.

## Secrets Security & Decryption
* GitHub Integration Access Tokens are stored encrypted in the database using AES.
* The `ENCRYPTION_KEY` is loaded from a production environment variable and never printed to server logs.
* General unhandled server exceptions do not leak stack traces, SQL syntax, or filesystem structures in production. Instead, they output a sanitized JSON message with a request ID.

## Docker Build Container Isolation
* All compilation tasks are run inside Docker containers with resource limits (CPU/Memory).
* Containers are run as a restricted user (`1000:1000`) instead of root.
* All Linux capabilities are dropped (`--cap-drop=ALL`), and `no-new-privileges` is set.
* Phase 8 build containment ensures Container B runs with `--network none` to prevent exfiltration during target scripts execution.
