import React from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Rocket, Info } from 'lucide-react';

export const Deployments: React.FC = () => {
  return (
    <DashboardLayout title="Deployments">
      <div className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold text-white">Deployments Log</h2>
          <p className="text-slate-400 text-sm">Real-time build logs and deployment history.</p>
        </div>

        <div className="glass-panel p-12 text-center flex flex-col items-center justify-center">
          <div className="w-14 h-14 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-4">
            <Rocket className="w-7 h-7 stroke-[1.5]" />
          </div>
          <h3 className="text-lg font-semibold text-slate-200 mb-1">No Deployment Records</h3>
          <p className="text-sm text-slate-400 max-w-sm mb-6">
            The automated Docker build and deployment engine will be introduced in Phase 5 & Phase 6.
          </p>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-400">
            <Info className="w-4 h-4 text-blue-400" />
            Phase 1 Placeholder
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};
