import React, { useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { StatCard } from '../components/StatCard';
import { EmptyState } from '../components/EmptyState';
import { NewProjectModal } from '../components/NewProjectModal';
import { useBackendStatus } from '../context/BackendStatusContext';
import { Plus, Terminal, ServerCheck, Info } from 'lucide-react';

export const Dashboard: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const { isConnected, appInfo } = useBackendStatus();

  return (
    <DashboardLayout title="Dashboard">
      <div className="space-y-8">
        {/* Welcome Section */}
        <div className="glass-panel p-8 relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="relative z-10 max-w-xl">
            <h2 className="text-3xl font-bold tracking-tight text-white mb-2">
              Welcome to AZHost
            </h2>
            <p className="text-slate-300 text-base leading-relaxed">
              Deploy your projects without the complexity. AZHost abstracts infrastructure into a simple, developer-friendly workflow.
            </p>
          </div>

          <div className="relative z-10">
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-lg shadow-blue-600/30 hover:scale-[1.02] active:scale-[0.98]"
            >
              <Plus className="w-5 h-5 stroke-[2.5]" />
              New Project
            </button>
          </div>

          {/* Background Decorative Gradient */}
          <div className="absolute -top-24 -right-24 w-72 h-72 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
        </div>

        {/* System Info Banner if connected */}
        {isConnected && appInfo && (
          <div className="bg-slate-900/40 border border-slate-800/80 rounded-xl p-4 flex items-center justify-between text-xs text-slate-400">
            <div className="flex items-center gap-3">
              <ServerCheck className="w-4 h-4 text-emerald-400" />
              <span>
                Connected to <strong className="text-slate-200">{appInfo.name}</strong> v{appInfo.version} ({appInfo.phase})
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">Status: {appInfo.status}</span>
            </div>
          </div>
        )}

        {/* Statistics Cards */}
        <div>
          <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Overview</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard title="Projects" value={0} iconName="projects" subtitle="Active developer projects" />
            <StatCard title="Deployments" value={0} iconName="deployments" subtitle="Total deployment builds" />
            <StatCard title="Live Sites" value={0} iconName="live-sites" subtitle="Active production domains" />
            <StatCard title="Successful Deployments" value={0} iconName="successful-deployments" subtitle="100% target reliability" />
          </div>
        </div>

        {/* Recent Activity Section */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Recent Activity</h3>
            <span className="text-xs text-slate-500 flex items-center gap-1 font-mono">
              <Terminal className="w-3.5 h-3.5" />
              Real-time audit log
            </span>
          </div>

          <EmptyState
            title="No deployments yet."
            description="Your deployments will appear here once you connect a repository and trigger a build."
          />
        </div>
      </div>

      <NewProjectModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </DashboardLayout>
  );
};
