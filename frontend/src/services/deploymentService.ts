import { ProjectDeployment, DeploymentListResponse } from '../types/deployment';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const deploymentService = {
  async createDeployment(projectId: string, buildId: string): Promise<ProjectDeployment> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ buildId }),
    });

    if (response.status === 409) {
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || 'Deployment cannot be started.');
      (error as any).status = 409;
      (error as any).code = errorData.error || 'CONFLICT';
      throw error;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to trigger deployment' }));
      throw new Error(errorData.message || 'Failed to trigger deployment');
    }

    return await response.json();
  },

  async getDeployments(projectId: string): Promise<DeploymentListResponse> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch project deployments');
    }

    return await response.json();
  },

  async getDeploymentById(projectId: string, deploymentId: string): Promise<ProjectDeployment> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments/${deploymentId}`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch deployment details');
    }

    return await response.json();
  },

  async cancelDeployment(projectId: string, deploymentId: string): Promise<ProjectDeployment> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments/${deploymentId}/cancel`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to cancel deployment');
    }

    return await response.json();
  },

  async rollbackToDeployment(projectId: string, deploymentId: string): Promise<ProjectDeployment> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/deployments/${deploymentId}/rollback`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to rollback deployment');
    }

    return await response.json();
  },
};
