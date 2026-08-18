import React, { useState } from 'react';
import { githubService } from '../services/githubService';
import { GitHubConnection, GitHubRepository } from '../types/github';
import { Project } from '../types/project';
import { GitHubBranchSelector } from './GitHubBranchSelector';
import { GitHubConnectionCard } from './GitHubConnectionCard';
import { GitHubRepositorySelector } from './GitHubRepositorySelector';

interface GitHubSourceCardProps {
  project: Project;
  onProjectUpdated: (updatedProject: Project) => void;
}

export const GitHubSourceCard: React.FC<GitHubSourceCardProps> = ({
  project,
  onProjectUpdated,
}) => {
  const [connection, setConnection] = useState<GitHubConnection | null>(null);
  const [isLinking, setIsLinking] = useState<boolean>(false);
  const [selectedRepo, setSelectedRepo] = useState<GitHubRepository | null>(null);
  const [selectedBranch, setSelectedBranch] = useState<string>('');
  const [saving, setSaving] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const isLinked = project.sourceType === 'GITHUB' && !!project.githubRepositoryId;

  const handleLinkSubmit = async () => {
    if (!selectedRepo) {
      setError('Please select a repository.');
      return;
    }
    if (!selectedBranch) {
      setError('Please select a branch.');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      const updated = await githubService.linkProject(project.id, {
        repositoryId: selectedRepo.id,
        branch: selectedBranch,
      });
      onProjectUpdated(updated);
      setIsLinking(false);
    } catch (err: any) {
      console.error('Failed to link GitHub repository:', err);
      setError(err.message || 'Failed to link GitHub repository.');
    } finally {
      setSaving(false);
    }
  };

  const handleUnlink = async () => {
    if (!window.confirm('Are you sure you want to disconnect GitHub source from this project?')) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const updated = await githubService.unlinkProject(project.id);
      onProjectUpdated(updated);
    } catch (err: any) {
      console.error('Failed to unlink GitHub repository:', err);
      setError(err.message || 'Failed to unlink GitHub repository.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl text-white">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-slate-800 rounded-lg text-indigo-400">
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
            </svg>
          </div>
          <div>
            <h3 className="text-lg font-semibold">Source Code Repository</h3>
            <p className="text-sm text-slate-400">Connect a GitHub repository to build and deploy your application.</p>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-900/40 border border-red-500/50 rounded-lg text-red-300 text-sm">
          {error}
        </div>
      )}

      {isLinked && !isLinking ? (
        <div className="space-y-4">
          <div className="p-4 bg-slate-950/60 border border-slate-800 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs uppercase tracking-wider font-semibold text-slate-400">Connected Repository</span>
              <span className="px-2.5 py-0.5 text-xs bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 rounded-full font-medium">GitHub</span>
            </div>
            
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-base font-semibold text-white">{project.githubRepositoryName || project.repositoryUrl}</h4>
                <div className="flex items-center space-x-4 mt-1 text-xs text-slate-400">
                  <div className="flex items-center space-x-1">
                    <span className="text-slate-500">Branch:</span>
                    <span className="font-mono text-indigo-300 font-medium">{project.githubBranch || project.repositoryBranch || 'main'}</span>
                  </div>
                  {project.githubCommitSha && (
                    <div className="flex items-center space-x-1">
                      <span className="text-slate-500">Commit:</span>
                      <span className="font-mono text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-800/40">
                        {project.githubCommitSha.substring(0, 7)}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {project.repositoryUrl && (
                <a
                  href={project.repositoryUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg transition-all flex items-center space-x-1.5"
                >
                  <span>View on GitHub</span>
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                </a>
              )}
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={() => setIsLinking(true)}
              disabled={saving}
              className="px-4 py-2 text-sm bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg transition-all"
            >
              Change Repository
            </button>
            <button
              onClick={handleUnlink}
              disabled={saving}
              className="px-4 py-2 text-sm bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 rounded-lg transition-all"
            >
              Disconnect Source
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-6">
          <GitHubConnectionCard onConnectionChange={setConnection} />

          {connection?.connected && (
            <div className="p-5 bg-slate-950/60 border border-slate-800 rounded-xl space-y-5">
              <h4 className="text-base font-semibold text-white">Link GitHub Repository</h4>

              <GitHubRepositorySelector
                selectedRepositoryId={selectedRepo?.id || project.githubRepositoryId}
                onSelect={(repo) => {
                  setSelectedRepo(repo);
                  setSelectedBranch(repo.defaultBranch);
                }}
              />

              {selectedRepo && (
                <GitHubBranchSelector
                  repositoryId={selectedRepo.id}
                  defaultBranch={selectedRepo.defaultBranch}
                  selectedBranch={selectedBranch}
                  onSelect={setSelectedBranch}
                />
              )}

              <div className="flex items-center space-x-3 pt-2">
                <button
                  onClick={handleLinkSubmit}
                  disabled={saving || !selectedRepo || !selectedBranch}
                  className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-medium rounded-lg shadow-lg shadow-indigo-600/30 transition-all text-sm"
                >
                  {saving ? 'Linking...' : 'Connect Repository'}
                </button>
                {isLinking && (
                  <button
                    onClick={() => setIsLinking(false)}
                    className="px-4 py-2.5 text-slate-400 hover:text-white text-sm"
                  >
                    Cancel
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
