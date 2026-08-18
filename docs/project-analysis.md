# AZHost Phase 3: Project Detection & Analysis Engine

This document provides a comprehensive technical specification for the **Project Detection & Analysis Engine** introduced in Phase 3 of AZHost.

---

## 1. Overview & Purpose

The purpose of Phase 3 is to safely analyze project metadata and manifest files to determine:

- **Framework**: React (+ Vite or CRA), Next.js, Vue (+ Vite or CLI), Angular, Static HTML, or Unknown.
- **Build Tool**: Vite, Next.js, Angular CLI, Create React App, Vue CLI, or None.
- **Package Manager**: npm, pnpm, yarn, bun, or UNKNOWN.
- **Programming Language**: TypeScript, JavaScript, or HTML.
- **Build Command**: Detected script string (e.g. `npm run build` or `pnpm build`).
- **Development Command**: Detected script string (e.g. `npm run dev`).
- **Output Directory**: `dist`, `build`, `.next`, or `.` (Root).
- **Node.js Version**: Detected version from `package.json` (`engines.node`), `.nvmrc`, or `.node-version`.
- **Detection Confidence**: `HIGH`, `MEDIUM`, or `LOW`.
- **Detection Warnings & Evidence**: Human-readable evidence and warning messages.

---

## 2. Security & Zero-Execution Boundary

> [!IMPORTANT]
> **Strict Non-Execution Contract**: The analysis engine inspects file contents strictly as untrusted text. It **NEVER** executes `package.json` scripts, `npm install`, `npm run build`, `git`, `docker`, or shell commands.

### Safety Measures:
1. **Zero Invocation of Execution APIs**: No `Runtime.getRuntime().exec()`, `ProcessBuilder`, or shell execution exist in the analysis module.
2. **Canonical Path Containment**: `LocalDirectorySourceReader` normalizes and resolves paths (`Path.toRealPath()`) against a server-configured root directory (`azhost.analysis.allowed-root-dir`). Path traversal (`../`), absolute paths, Windows drive escapes, and symlinks/junctions pointing outside the allowed root are strictly blocked.
3. **Known-File Allowlist**: The analyzer only inspects explicit manifest files (`package.json`, `package-lock.json`, `pnpm-lock.yaml`, `yarn.lock`, `bun.lock`, `vite.config.*`, `next.config.*`, `angular.json`, `.nvmrc`, `.node-version`, `tsconfig.json`, `index.html`).
4. **Excluded Directories**: Automatically ignores `node_modules`, `.git`, `dist`, `build`, `.next`, `target`.
5. **Resource Limits**: Max file read size limit (500 KB) and max inspected file count limit (100 files).

---

## 3. Detection Rules & Precedence

### Framework Detection
- **Next.js**: `package.json` contains `"next"` dependency OR scripts containing `next dev`/`next build`/`next start` -> `HIGH` confidence.
- **Angular**: `package.json` contains `"@angular/core"` AND `angular.json` exists -> `HIGH` confidence.
- **React + Vite**: `package.json` contains `"react"` AND (`"vite"` dependency OR `vite.config.*`) -> `HIGH` confidence.
- **React CRA**: `package.json` contains `"react"` AND `"react-scripts"` -> `HIGH` confidence.
- **Vue + Vite**: `package.json` contains `"vue"` AND (`"vite"` dependency OR `vite.config.*`) -> `HIGH` confidence.
- **Static**: `index.html` exists AND no framework dependencies -> `HIGH` confidence.
- **Unknown**: Fallback when no framework rules match -> `LOW` confidence.

### Package Manager Precedence
1. `"packageManager"` field in `package.json` (e.g. `"pnpm@10.0.0"`).
2. Lockfile detection: `package-lock.json` (`npm`), `pnpm-lock.yaml` (`pnpm`), `yarn.lock` (`yarn`), `bun.lock`/`bun.lockb` (`bun`).
3. Fallback: `UNKNOWN` (`LOW` confidence). Warnings are reported if lockfiles and declared package managers conflict.

### Node.js Version Precedence
1. `engines.node` in `package.json`.
2. `.nvmrc`.
3. `.node-version`.
Warnings are generated if conflicting versions are declared across files.

---

## 4. API Endpoints

### `POST /api/projects/{id}/analyze`
- **200 OK**: Analysis completed and persisted.
- **409 Conflict**: Returned when physical project source files are not present on disk.
  ```json
  {
    "timestamp": "2026-08-18T20:00:00.000Z",
    "status": 409,
    "error": "PROJECT_SOURCE_NOT_AVAILABLE",
    "message": "Project source is not available for analysis yet.",
    "path": "/api/projects/.../analyze"
  }
  ```
- **404 Not Found**: Project ID does not exist.

### `GET /api/projects/{id}/analysis`
- **200 OK**: Latest persisted analysis result.
- **404 Not Found**: Analysis result does not exist.
