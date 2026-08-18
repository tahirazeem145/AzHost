import { ProjectAnalysisResult } from '../types/projectAnalysis';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const projectAnalysisService = {
  async analyzeProject(projectId: string): Promise<ProjectAnalysisResult> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/analyze`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (response.status === 409) {
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || 'Project source is not available for analysis yet.');
      (error as any).status = 409;
      (error as any).code = errorData.error || 'PROJECT_SOURCE_NOT_AVAILABLE';
      throw error;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to analyze project' }));
      throw new Error(errorData.message || 'Failed to analyze project');
    }

    return await response.json();
  },

  async getLatestAnalysis(projectId: string): Promise<ProjectAnalysisResult | null> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/analysis`, {
      headers: { 'Accept': 'application/json' },
    });

    if (response.status === 404) {
      return null;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to fetch analysis' }));
      throw new Error(errorData.message || 'Failed to fetch analysis');
    }

    return await response.json();
  },
};
