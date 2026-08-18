export interface GitHubConnection {
  connected: boolean;
  githubUsername?: string;
  avatarUrl?: string;
  connectedAt?: string;
}

export interface GitHubRepository {
  id: number;
  name: string;
  fullName: string;
  private: boolean;
  defaultBranch: string;
  htmlUrl: string;
  updatedAt: string;
}

export interface GitHubBranch {
  name: string;
  protected: boolean;
}

export interface GitHubCommit {
  sha: string;
  message: string;
  authorName: string;
  date: string;
}

export interface LinkGitHubRequest {
  repositoryId: number;
  branch: string;
}
