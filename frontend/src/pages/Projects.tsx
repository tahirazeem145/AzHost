import React, { useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { FolderGit2, Plus, Info } from 'lucide-react';
import { NewProjectModal } from '../components/NewProjectModal';

export const Projects: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <DashboardLayout title="Projects">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-white">Your Projects</h2>
            <p className="text-slate-400 text-sm">Manage and monitor all hosted applications.</p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-lg transition-colors shadow-md shadow-blue-600/20"
          >
            <Plus className="w-4 h-4" />
            New Project
          </button>
        </div>

        <div className="glass-panel p-12 text-center flex flex-col items-center justify-center">
          <div className="w-14 h-14 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-4">
            <FolderGit2 className="w-7 h-7 stroke-[1.5]" />
          </div>
          <h3 className="text-lg font-semibold text-slate-200 mb-1">No Projects Found</h3>
          <p className="text-sm text-slate-400 max-w-sm mb-6">
            Project management and Git repository integration will be enabled in Phase 3.
          </p>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-md bg-slate-950 border border-slate-800 text-xs text-slate-400">
            <Info className="w-4 h-4 text-blue-400" />
            Phase 1 Placeholder
          </div>
        </div>
      </div>

      <NewProjectModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </DashboardLayout>
  );
};
