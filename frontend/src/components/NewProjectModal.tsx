import React from 'react';
import { X, Sparkles, FolderPlus } from 'lucide-react';

interface NewProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NewProjectModal: React.FC<NewProjectModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-200 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="w-12 h-12 rounded-xl bg-blue-950/80 border border-blue-800/60 flex items-center justify-center text-blue-400 mb-4">
          <FolderPlus className="w-6 h-6" />
        </div>

        <div className="flex items-center gap-2 mb-2">
          <h3 className="text-xl font-bold text-slate-100">Create New Project</h3>
          <span className="text-xs px-2 py-0.5 rounded-full bg-blue-950 text-blue-400 border border-blue-800/60 font-semibold">
            Phase 3
          </span>
        </div>

        <p className="text-slate-400 text-sm mb-6 leading-relaxed">
          Project creation, repository detection, and deployment engine features will be available in <strong className="text-slate-200">Phase 3: Project Management</strong>.
        </p>

        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 text-xs text-slate-400 space-y-2 mb-6">
          <div className="flex items-center gap-2 text-slate-300 font-medium">
            <Sparkles className="w-4 h-4 text-blue-400" />
            Upcoming Features:
          </div>
          <ul className="list-disc list-inside space-y-1 text-slate-400 pl-1">
            <li>GitHub & Git repository import</li>
            <li>Automatic framework detection (React, Next.js, Vite, etc.)</li>
            <li>Environment variable management</li>
            <li>Instant deployment trigger</li>
          </ul>
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-medium transition-colors shadow-lg shadow-blue-600/30"
        >
          Got it
        </button>
      </div>
    </div>
  );
};
