# AZHost Operations Guide

This guide covers daily maintenance, system monitoring, troubleshooting, and alerting strategies.

## Essential Metrics to Monitor
From `/actuator/prometheus`, monitor:
* **Active/Queued builds**: `azhost_active_builds` & `azhost_queued_builds`. Alert if queue size remains > 10 for more than 5 minutes.
* **Build / Deployment success rate**: `azhost_builds_total` & `azhost_deployments_total` with tags `status=success` vs `status=failed`.
* **Docker errors**: `azhost_docker_create_failures`, `azhost_docker_start_failures`, `azhost_docker_cleanup_failures`. Any count > 0 requires immediate attention.
* **GitHub webhooks**: `azhost_webhooks_received`, `azhost_webhooks_rejected`.

## Troubleshooting Using Request Correlation IDs
* Every incoming HTTP request is assigned a unique `X-Request-ID`.
* Look up this ID in the application logs to trace the full flow of log lines:
  ```bash
  grep "reqId=550e8400-e29b-41d4-a716-446655440000" azhost-backend.log
  ```
* All internal exceptions display the request ID in the sanitized production response.

## Safe Restart Procedure
1. Gracefully drain existing builds by blocking new webhook processing:
   ```bash
   # Temporarily disable auto-deploys via DB or environment
   ```
2. Wait for `azhost_active_builds` metric to reach 0.
3. Restart the container stack:
   ```bash
   docker compose -f docker-compose.production.yml restart
   ```
