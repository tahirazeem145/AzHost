export type DeploymentStatus =
  | 'QUEUED'
  | 'PREPARING'
  | 'EXTRACTING'
  | 'VALIDATING'
  | 'PUBLISHING'
  | 'SUCCESS'
  | 'FAILED'
  | 'CANCELLED';

export interface ProjectDeployment {
  id: string;
  projectId: string;
  buildId: string;
  artifactId: string;
  status: DeploymentStatus;
  deploymentUrl?: string;
  createdAt: string;
  publishedAt?: string;
  failedAt?: string;
  errorMessage?: string;
  active: boolean;
}

export interface DeploymentListResponse {
  deployments: ProjectDeployment[];
}
