export type ProjectFramework =
  | 'UNKNOWN'
  | 'REACT'
  | 'VITE'
  | 'NEXT_JS'
  | 'VUE'
  | 'ANGULAR'
  | 'STATIC';

export type ProjectSourceType = 'GITHUB' | 'UPLOAD' | 'LOCAL';

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface Project {
  id: string;
  name: string;
  slug: string;
  description?: string;
  framework: ProjectFramework;
  sourceType: ProjectSourceType;
  repositoryUrl?: string;
  repositoryBranch?: string;
  githubRepositoryId?: number;
  githubRepositoryName?: string;
  githubBranch?: string;
  githubCommitSha?: string;
  autoDeploy?: boolean;
  autoDeployBranch?: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}


export interface CreateProjectRequest {
  name: string;
  description?: string;
  framework: ProjectFramework;
  sourceType: ProjectSourceType;
  repositoryUrl?: string;
  repositoryBranch?: string;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string;
  framework: ProjectFramework;
  repositoryUrl?: string;
  repositoryBranch?: string;
  status: ProjectStatus;
  autoDeploy?: boolean;
  autoDeployBranch?: string;
}

export interface AutoDeploySettingsRequest {
  autoDeploy: boolean;
  autoDeployBranch?: string;
}

export interface ProjectListResponse {
  projects: Project[];
  totalCount: number;
}

