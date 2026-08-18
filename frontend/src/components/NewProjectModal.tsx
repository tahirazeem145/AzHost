import React, { useState } from 'react';
import { CreateProjectRequest, ProjectFramework, ProjectSourceType } from '../types/project';
import { X, FolderPlus, Github, UploadCloud, Laptop, Loader2, Info } from 'lucide-react';

interface NewProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateProjectRequest) => Promise<void>;
}

export const NewProjectModal: React.FC<NewProjectModalProps> = ({ isOpen, onClose, onSubmit }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [framework, setFramework] = useState<ProjectFramework>('REACT');
  const [sourceType, setSourceType] = useState<ProjectSourceType>('GITHUB');
  const [repositoryUrl, setRepositoryUrl] = useState('');
  const [repositoryBranch, setRepositoryBranch] = useState('main');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Project name is required');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      await onSubmit({
        name: name.trim(),
        description: description.trim() || undefined,
        framework,
        sourceType,
        repositoryUrl: sourceType === 'GITHUB' ? repositoryUrl.trim() : undefined,
        repositoryBranch: sourceType === 'GITHUB' ? repositoryBranch.trim() : undefined,
      });

      // Reset form
      setName('');
      setDescription('');
      setFramework('REACT');
      setSourceType('GITHUB');
      setRepositoryUrl('');
      setRepositoryBranch('main');
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create project');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          disabled={isSubmitting}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-200 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-blue-950 border border-blue-800/60 flex items-center justify-center text-blue-400">
            <FolderPlus className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-slate-100">Create New Project</h3>
            <p className="text-xs text-slate-400">Configure project details and repository source</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-950/80 border border-rose-800 text-rose-300 text-xs font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Project Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Project Name *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. TripNest 2.0"
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Description
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Brief description of what your application does"
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors resize-none"
            />
          </div>

          {/* Framework Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Framework *
            </label>
            <select
              value={framework}
              onChange={(e) => setFramework(e.target.value as ProjectFramework)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-100 focus:outline-none focus:border-blue-500 transition-colors"
            >
              <option value="REACT">React (Vite / CRA)</option>
              <option value="VITE">Vite Static / SPA</option>
              <option value="NEXT_JS">Next.js</option>
              <option value="VUE">Vue.js</option>
              <option value="ANGULAR">Angular</option>
              <option value="STATIC">Static HTML / JS</option>
              <option value="UNKNOWN">Other / Custom</option>
            </select>
          </div>

          {/* Source Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Project Source *
            </label>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setSourceType('GITHUB')}
                className={`p-3 rounded-lg border text-xs font-semibold flex flex-col items-center gap-1.5 transition-colors ${
                  sourceType === 'GITHUB'
                    ? 'bg-blue-950/80 border-blue-600 text-blue-400'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <Github className="w-4 h-4" />
                GitHub
              </button>

              <button
                type="button"
                onClick={() => setSourceType('UPLOAD')}
                className={`p-3 rounded-lg border text-xs font-semibold flex flex-col items-center gap-1.5 transition-colors ${
                  sourceType === 'UPLOAD'
                    ? 'bg-blue-950/80 border-blue-600 text-blue-400'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <UploadCloud className="w-4 h-4" />
                Zip Upload
              </button>

              <button
                type="button"
                onClick={() => setSourceType('LOCAL')}
                className={`p-3 rounded-lg border text-xs font-semibold flex flex-col items-center gap-1.5 transition-colors ${
                  sourceType === 'LOCAL'
                    ? 'bg-blue-950/80 border-blue-600 text-blue-400'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'
                }`}
              >
                <Laptop className="w-4 h-4" />
                Local Folder
              </button>
            </div>
          </div>

          {/* Conditional Fields Based on Source */}
          {sourceType === 'GITHUB' && (
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">
                  Repository URL
                </label>
                <input
                  type="url"
                  value={repositoryUrl}
                  onChange={(e) => setRepositoryUrl(e.target.value)}
                  placeholder="https://github.com/username/repository"
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">
                  Branch
                </label>
                <input
                  type="text"
                  value={repositoryBranch}
                  onChange={(e) => setRepositoryBranch(e.target.value)}
                  placeholder="main"
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
          )}

          {sourceType === 'UPLOAD' && (
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 flex items-center gap-3 text-xs text-amber-400">
              <Info className="w-4 h-4 flex-shrink-0" />
              <span>Direct ZIP Upload support will be enabled in a future AZHost phase.</span>
            </div>
          )}

          {sourceType === 'LOCAL' && (
            <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 flex items-center gap-3 text-xs text-amber-400">
              <Info className="w-4 h-4 flex-shrink-0" />
              <span>Local project folder integration will be enabled in a future AZHost phase.</span>
            </div>
          )}

          {/* Action Buttons */}
          <div className="pt-4 flex items-center justify-end gap-3 border-t border-slate-800/80">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg font-medium text-sm transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-lg font-semibold text-sm transition-colors shadow-lg shadow-blue-600/30 disabled:opacity-50"
            >
              {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
              Create Project
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
