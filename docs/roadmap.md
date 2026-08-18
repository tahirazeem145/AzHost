# AZHost Product Master Roadmap (Phases 1–10)

This roadmap outlines the 10-phase progression for building **AZHost**, from initial foundation to full-scale production hosting platform.

---

## Phase 1 — Foundation & Architecture (Current Phase)
- Expand dashboard analytics cards and interactive statistics tracking.
- Build system activity feeds, filterable logs, and UI dark/light mode toggles.
- Add user profile management UI and notification banners.

## Phase 3 — Project Management & Repository Import [COMPLETED]
- Implement `projects` and `environment_variables` database entities and REST endpoints.
- Support manual repository linking (Git HTTPS/SSH clone URLs).
- Build environment variable configuration UI with encrypted storage.

## Phase 4 — Project & Framework Detection Engine [COMPLETED]
- Implement workspace scanner detecting Node.js (React, Vite, Next.js, Vue), Java (Spring Boot, Maven, Gradle), and Python frameworks.
- Auto-generate recommended build commands and output directory defaults.

## Phase 5 — Build Engine Infrastructure
- Create isolated build worker runner using Docker containers.
- Implement streaming log capture (`deployment_logs` entity) for real-time stdout/stderr visualization in the UI.

## Phase 6 — Deployment Engine & Static Hosting
- Implement target deployment routing and static asset serving engine.
- Create container lifecycle manager for spinning up project containers.
- Implement basic instant rollback mechanism to previous deployment builds.

## Phase 7 — GitHub OAuth & Webhook Integration
- Implement GitHub OAuth 2.0 authentication flow (`github_connections` entity).
- Support automated repository browsing and branch selection.
- Implement GitHub Webhook listeners for automatic push-to-deploy triggers.

## Phase 8 — Automation, Self-Healing & Recovery
- Implement automated container health monitors and auto-restart policies.
- Build deployment queue worker management for concurrent builds.
- Implement failure alerts and graceful rollback strategies.

## Phase 9 — Custom Domains & SSL Automation
- Implement custom domain mapping (`domains` entity) and CNAME verification.
- Automate Let's Encrypt SSL certificate generation via ACME protocol & Nginx dynamic proxying.

## Phase 10 — Production Hosting & Cloud Expansion
- Prepare cloud deployment manifests (VPS / AWS EC2 / DigitalOcean Droplets).
- Implement multi-node container distribution and metrics monitoring.
- Enterprise-grade backup strategy and zero-downtime platform upgrades.
