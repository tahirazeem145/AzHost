import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { BackendStatusState, HealthResponse, InfoResponse } from '../types';
import { apiService } from '../services/api';

const BackendStatusContext = createContext<BackendStatusState | undefined>(undefined);

export const BackendStatusProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [healthInfo, setHealthInfo] = useState<HealthResponse | null>(null);
  const [appInfo, setAppInfo] = useState<InfoResponse | null>(null);
  const [lastChecked, setLastChecked] = useState<Date | null>(null);

  const checkStatus = useCallback(async () => {
    setIsLoading(true);
    try {
      const [health, info] = await Promise.all([
        apiService.getHealth(),
        apiService.getInfo().catch(() => null)
      ]);

      if (health && health.status === 'UP') {
        setIsConnected(true);
        setHealthInfo(health);
        setAppInfo(info);
      } else {
        setIsConnected(false);
      }
    } catch {
      setIsConnected(false);
      setHealthInfo(null);
      setAppInfo(null);
    } finally {
      setIsLoading(false);
      setLastChecked(new Date());
    }
  }, []);

  useEffect(() => {
    checkStatus();
    // Poll health status every 30 seconds
    const interval = setInterval(checkStatus, 30000);
    return () => clearInterval(interval);
  }, [checkStatus]);

  return (
    <BackendStatusContext.Provider
      value={{
        isConnected,
        isLoading,
        healthInfo,
        appInfo,
        lastChecked,
        checkStatus,
      }}
    >
      {children}
    </BackendStatusContext.Provider>
  );
};

export const useBackendStatus = () => {
  const context = useContext(BackendStatusContext);
  if (!context) {
    throw new Error('useBackendStatus must be used within a BackendStatusProvider');
  }
  return context;
};
