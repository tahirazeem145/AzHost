# Nginx Architecture Foundation

## Phase 1 Purpose
In Phase 1, Nginx serves as the production web server for static React bundle delivery and functions as an API reverse proxy directing requests to the Spring Boot backend (`http://backend:8080/api/`).

## Future Hosting Architecture Roadmap
In later phases (Phase 6-10), Nginx will serve as the entry edge router for dynamic project hosting:

```text
Incoming User Request (e.g. app.user-project.azhost.dev)
                     ↓
               Nginx Gateway
                     ↓
          AZHost Routing Layer
                     ↓
   Target User Container / Dynamic Subdomain
```

Key features prepared for future phases:
1. Dynamic Subdomain Proxying (e.g., `*.azhost.local` / `*.azhost.io`)
2. Automatic Let's Encrypt / Certbot SSL termination
3. WebSocket proxying for real-time build and deployment logs
4. Custom domain CNAME mapping
