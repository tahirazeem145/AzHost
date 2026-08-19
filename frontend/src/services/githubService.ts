import { GitHubBranch, GitHubConnection, GitHubRepository, LinkGitHubRequest } from '../types/github';
import { AutoDeploySettingsRequest, Project } from '../types/project';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const githubService = {
  async getConnection(): Promise<GitHubConnection> {
    const response = await fetch(`${API_BASE_URL}/api/github/connection`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch GitHub connection status: ${response.status}`);
    }
    return await response.json();
  },

  async getConnectUrl(): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/api/github/connect`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to initiate GitHub OAuth connect: ${response.status}`);
    }
    const data = await response.json();
    return data.url;
  },

  async disconnect(): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/api/github/connection`, {
      method: 'DELETE',
    });
    if (!response.ok && response.status !== 204) {
      throw new Error(`Failed to disconnect GitHub account: ${response.status}`);
    }
  },

  async getRepositories(): Promise<GitHubRepository[]> {
    const response = await fetch(`${API_BASE_URL}/api/github/repositories`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch GitHub repositories: ${response.status}`);
    }
    return await response.json();
  },

  async getBranches(repositoryId: number): Promise<GitHubBranch[]> {
    const response = await fetch(`${API_BASE_URL}/api/github/repositories/${repositoryId}/branches`, {
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch GitHub branches: ${response.status}`);
    }
    return await response.json();
  },

  async linkProject(projectId: string, request: LinkGitHubRequest): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/github`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to link GitHub repository: ${response.status}`);
    }
    return await response.json();
  },

  async unlinkProject(projectId: string): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/github`, {
      method: 'DELETE',
      headers: { 'Accept': 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`Failed to unlink GitHub repository: ${response.status}`);
    }
    return await response.json();
  },

  /**
   * Update auto-deploy settings for a project.
   * Calls PATCH /api/projects/:id/github/auto-deploy
   */
  async updateAutoDeploySettings(projectId: string, settings: AutoDeploySettingsRequest): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/github/auto-deploy`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(settings),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to update auto-deploy settings: ${response.status}`);
    }
    return await response.json();
  },

  /**
   * Set or rotate the per-project GitHub webhook secret.
   * The secret is only ever sent once to the backend during setup;
   * it is stored encrypted and never returned to the frontend.
   */
  async setWebhookSecret(projectId: string, webhookSecret: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/github/webhook-secret`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ webhookSecret }),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Failed to set webhook secret: ${response.status}`);
    }
  },
};
