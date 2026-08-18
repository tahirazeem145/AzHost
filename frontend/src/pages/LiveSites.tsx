import React from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Globe, Info } from 'lucide-react';

export const LiveSites: React.FC = () => {
  return (
    <DashboardLayout title="Live Sites">
      <div className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold text-white">Live Sites & Domains</h2>
          <p className="text-slate-400 text-sm">Active production endpoints and custom domain routes.</p>
        </div>

        <div className="glass-panel p-12 text-center flex flex-col items-center justify-center">
          <div className="w-14 h-14 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-4">
            <Globe className="w-7 h-7 stroke-[1.5]" />
          </div>
          <h3 className="text-lg font-semibold text-slate-200 mb-1">No Active Live Sites</h3>
          <p className="text-sm text-slate-400 max-w-sm mb-6">
            Dynamic Nginx domain routing and SSL certificate management will be implemented in Phase 9 & Phase 10.
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
