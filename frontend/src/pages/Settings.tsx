import React from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Settings as SettingsIcon, Shield, Server, Database, Info } from 'lucide-react';
import { useBackendStatus } from '../context/BackendStatusContext';

export const Settings: React.FC = () => {
  const { isConnected, appInfo, healthInfo } = useBackendStatus();

  return (
    <DashboardLayout title="Settings">
      <div className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold text-white">Platform Settings</h2>
          <p className="text-slate-400 text-sm">System configuration, security settings, and environment info.</p>
        </div>

        {/* System Diagnostics Card */}
        <div className="glass-panel p-6 space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
            <Server className="w-5 h-5 text-blue-400" />
            <h3 className="text-base font-semibold text-white">Platform Architecture Status</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 space-y-1">
              <span className="text-slate-500 text-xs font-medium uppercase">Frontend Engine</span>
              <p className="text-slate-200 font-semibold">React 18 + Vite + TypeScript</p>
              <p className="text-xs text-slate-400">Tailwind CSS Design System</p>
            </div>

            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 space-y-1">
              <span className="text-slate-500 text-xs font-medium uppercase">Backend Engine</span>
              <p className="text-slate-200 font-semibold">
                Spring Boot 3.2 (Java 17)
              </p>
              <p className="text-xs text-slate-400">
                Status: {isConnected ? '● Connected' : '● Offline'}
              </p>
            </div>

            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 space-y-1">
              <span className="text-slate-500 text-xs font-medium uppercase">Database Foundation</span>
              <p className="text-slate-200 font-semibold">PostgreSQL 15 (Docker)</p>
              <p className="text-xs text-slate-400">Entity: User (UUID, Hashes)</p>
            </div>

            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 space-y-1">
              <span className="text-slate-500 text-xs font-medium uppercase">Current Release</span>
              <p className="text-slate-200 font-semibold">{appInfo?.version || '0.1.0'} ({appInfo?.phase || 'Phase 1'})</p>
              <p className="text-xs text-slate-400">Environment: {appInfo?.status || 'development'}</p>
            </div>
          </div>
        </div>

        {/* Security & Authentication Notice */}
        <div className="glass-panel p-6 space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
            <Shield className="w-5 h-5 text-blue-400" />
            <h3 className="text-base font-semibold text-white">Security & Access Control</h3>
          </div>

          <p className="text-sm text-slate-400 leading-relaxed">
            Full user authentication, OAuth2 providers (GitHub), role-based access control, and API token management will be configured in future security phases.
          </p>
        </div>
      </div>
    </DashboardLayout>
  );
};
