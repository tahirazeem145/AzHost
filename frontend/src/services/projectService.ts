import {
  Project,
  CreateProjectRequest,
  UpdateProjectRequest,
  ProjectListResponse,
} from '../types/project';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const projectService = {
  async getProjects(searchQuery?: string): Promise<ProjectListResponse> {
    const url = searchQuery
      ? `${API_BASE_URL}/api/projects?search=${encodeURIComponent(searchQuery)}`
      : `${API_BASE_URL}/api/projects`;

    const response = await fetch(url, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to fetch projects' }));
      throw new Error(errorData.message || 'Failed to fetch projects');
    }

    return await response.json();
  },

  async getProjectCount(): Promise<number> {
    const response = await fetch(`${API_BASE_URL}/api/projects/count`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      throw new Error('Failed to fetch project count');
    }

    const data = await response.json();
    return data.count || 0;
  },

  async getProjectById(id: string): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`, {
      headers: { 'Accept': 'application/json' },
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Project not found' }));
      throw new Error(errorData.message || 'Project not found');
    }

    return await response.json();
  },

  async createProject(data: CreateProjectRequest): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to create project' }));
      throw new Error(errorData.message || 'Failed to create project');
    }

    return await response.json();
  },

  async updateProject(id: string, data: UpdateProjectRequest): Promise<Project> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to update project' }));
      throw new Error(errorData.message || 'Failed to update project');
    }

    return await response.json();
  },

  async deleteProject(id: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/api/projects/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to delete project' }));
      throw new Error(errorData.message || 'Failed to delete project');
    }
  },
};
