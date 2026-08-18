export interface HealthResponse {
  status: string;
  service: string;
}

export interface InfoResponse {
  name: string;
  version: string;
  phase: string;
  status: string;
}

export interface StatCardProps {
  title: string;
  value: number | string;
  iconName: string;
  description?: string;
}

export interface BackendStatusState {
  isConnected: boolean;
  isLoading: boolean;
  healthInfo: HealthResponse | null;
  appInfo: InfoResponse | null;
  lastChecked: Date | null;
  checkStatus: () => Promise<void>;
}
