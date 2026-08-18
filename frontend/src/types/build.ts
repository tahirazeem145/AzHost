import { ProjectFramework } from './project';

export type BuildStatus =
  | 'QUEUED'
  | 'PREPARING'
  | 'INSTALLING'
  | 'BUILDING'
  | 'SUCCESS'
  | 'FAILED'
  | 'TIMEOUT'
  | 'CANCELLED';

export interface ProjectBuild {
  id: string;
  projectId: string;
  status: BuildStatus;
  framework: ProjectFramework;
  packageManager: string;
  nodeVersion: string;
  buildCommand?: string;
  outputDirectory: string;
  artifactId?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  exitCode?: number;
  errorMessage?: string;
  createdAt: string;
}

export interface BuildLogResponse {
  buildId: string;
  status: BuildStatus;
  logs: string[];
  truncated: boolean;
}
