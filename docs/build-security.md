# AZHost Phase 4: Build Security & Threat Model

This document outlines the security architecture protecting the AZHost platform during untrusted project build execution.

---

## 1. Zero Host Execution Principle

User project code (`package.json` scripts, postinstall hooks, custom build binaries) is treated as **UNTRUSTED**. User code is **NEVER** executed directly on the host operating system (Windows 11 or Linux VPS).

All command executions are delegated to isolated, resource-constrained Docker containers.

---

## 2. ZIP-Slip & Archive Security

`ZipSourceProvider` enforces strict path safety during archive extraction:

1. **Path Normalization**: Validates that every extracted entry satisfies `entryPath.normalize().startsWith(targetWorkspaceDir)`.
2. **Rejection Rules**: Rejects absolute paths (`/`), backslashes (`\`), Windows drive letters (`C:`), UNC paths (`\\`), and `..` path traversal sequences.
3. **Hard Caps**:
   - Max compressed file size: `100 MB`.
   - Max uncompressed total size: `500 MB`.
   - Max file count: `10,000 files`.

---

## 3. Network Isolation

To prevent malicious build scripts from exfiltrating environment keys, scanning local host networks, or attacking internal services:

- **Stage 1 (Install)**: Container A runs with restricted registry network access.
- **Stage 2 (Build)**: Container B executes with **`--network none`** (100% offline).

---

## 4. Structured Non-Shell Execution

Command strings are never interpolated into shell interpreters (`sh -c` or `cmd.exe`). Commands are executed as structured exec arrays:

```java
List<String> installCmd = List.of("npm", "ci", "--ignore-scripts");
List<String> buildCmd = List.of("npm", "run", "build");
```

Command injection via malicious script names is rendered impossible.
