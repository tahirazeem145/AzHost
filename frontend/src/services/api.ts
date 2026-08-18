import { HealthResponse, InfoResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export const apiService = {
  async getHealth(): Promise<HealthResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/health`, {
        headers: {
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP Error status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.warn('Backend health check failed:', error);
      throw error;
    }
  },

  async getInfo(): Promise<InfoResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/api/info`, {
        headers: {
          'Accept': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP Error status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.warn('Backend info fetch failed:', error);
      throw error;
    }
  },
};
