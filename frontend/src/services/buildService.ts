import { ProjectBuild, BuildLogResponse } from '../types/build';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const buildService = {
  async startBuild(projectId: string): Promise<ProjectBuild> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/builds`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });

    if (response.status === 409) {
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || 'Build cannot be started.');
      (error as any).status = 409;
      (error as any).code = errorData.error || 'CONFLICT';
      throw error;
    }

    if (response.status === 503) {
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || 'Build engine is unavailable.');
      (error as any).status = 503;
      (error as any).code = 'BUILD_ENGINE_UNAVAILABLE';
      throw error;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to start build' }));
      throw new Error(errorData.message || 'Failed to start build');
    }

    return await response.json();
  },

  async getBuilds(projectId: string): Promise<ProjectBuild[]> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/builds`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch project builds');
    }

    return await response.json();
  },

  async getBuildById(projectId: string, buildId: string): Promise<ProjectBuild> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/builds/${buildId}`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch build details');
    }

    return await response.json();
  },

  async getBuildLogs(projectId: string, buildId: string): Promise<BuildLogResponse> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/builds/${buildId}/logs`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch build logs');
    }

    return await response.json();
  },

  async cancelBuild(projectId: string, buildId: string): Promise<ProjectBuild> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/builds/${buildId}/cancel`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to cancel build');
    }

    return await response.json();
  },
};
