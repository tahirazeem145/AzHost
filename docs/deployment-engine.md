# AZHost Phase 5: Artifact Deployment & Static Hosting Engine Specification

This document details the technical architecture and operation of the **Phase 5 Deployment Engine** in AZHost.

---

## 1. Overview & Objectives

Phase 5 publishes Phase 4 compiled build artifacts (`artifacts/{artifactId}.zip`) as static web sites.

- **Primary Goal**: Extract build output, validate static assets (requiring `index.html`), publish to immutable directories (`deployments/{deploymentId}`), and serve static HTTP requests.
- **Scope Restriction**: Phase 5 DOES NOT support custom domains, Let's Encrypt SSL, server-side app scripts (.php/.py), or Kubernetes.

---

## 2. Deployment Pipeline & State Machine

```text
Phase 4 Build Artifact (artifacts/{artifactId}.zip)
                 │
                 ▼
       [ State: QUEUED ]
                 │
                 ▼
      [ State: PREPARING ] (Allocate temp workspace: deployment-workspaces/{id})
                 │
                 ▼
      [ State: EXTRACTING ] (Stream ZIP extraction & ZIP-Slip containment check)
                 │
                 ▼
      [ State: VALIDATING ] (Validate file limits, no executable scripts, index.html present)
                 │
                 ▼
      [ State: PUBLISHING ] (Copy assets to immutable deployments/{deploymentId})
                 │
                 ▼
      [ State: SUCCESS ] (Update project active_deployment_id & generate live URL)
```

---

## 3. Deployment Directory Immutability & Rollback

- **Immutability**: Once a deployment reaches `SUCCESS`, `deployments/{deploymentId}` is treated as immutable and is never modified.
- **Rollback**: Rollback to a previous successful deployment is achieved by setting `active_deployment_id` in the `projects` table to point to the target `DeploymentEntity`. Existing deployment directories are preserved.

---

## 4. API Endpoints

- `POST /api/projects/{projectId}/deployments`: Triggers artifact deployment (`202 Accepted`). Requires `{ "buildId": "uuid" }`.
- `GET /api/projects/{projectId}/deployments`: Returns list of project deployment records (`200 OK`).
- `GET /api/projects/{projectId}/deployments/{deploymentId}`: Returns deployment details (`200 OK`).
- `POST /api/projects/{projectId}/deployments/{deploymentId}/cancel`: Cancels an active deployment (`200 OK`).
- `POST /api/projects/{projectId}/deployments/{deploymentId}/rollback`: Sets deployment as active (`200 OK`).
- `GET /api/deployments/{deploymentId}/files/**`: Serves static assets with safe MIME types (`text/html`, `text/css`, `text/javascript`, etc.) and canonical path traversal protection.
