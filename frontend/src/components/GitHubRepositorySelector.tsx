import React, { useEffect, useState } from 'react';
import { githubService } from '../services/githubService';
import { GitHubRepository } from '../types/github';

interface GitHubRepositorySelectorProps {
  onSelect: (repository: GitHubRepository) => void;
  selectedRepositoryId?: number;
}

export const GitHubRepositorySelector: React.FC<GitHubRepositorySelectorProps> = ({
  onSelect,
  selectedRepositoryId,
}) => {
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>('');

  useEffect(() => {
    const fetchRepos = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await githubService.getRepositories();
        setRepositories(data);
      } catch (err: any) {
        console.error('Failed to fetch repositories:', err);
        setError(err.message || 'Failed to load GitHub repositories.');
      } finally {
        setLoading(false);
      }
    };
    fetchRepos();
  }, []);

  const filteredRepositories = repositories.filter(
    (repo) =>
      repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      repo.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="p-4 bg-slate-900/60 border border-slate-800 rounded-xl animate-pulse space-y-3">
        <div className="h-4 bg-slate-800 rounded w-1/4"></div>
        <div className="h-10 bg-slate-800 rounded w-full"></div>
        <div className="h-20 bg-slate-800 rounded w-full"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-900/30 border border-red-500/40 rounded-xl text-red-300 text-sm">
        {error}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <label className="block text-sm font-medium text-slate-300">Select GitHub Repository</label>
      
      <div className="relative">
        <input
          type="text"
          placeholder="Search repositories..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 text-sm"
        />
      </div>

      <div className="max-h-60 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
        {filteredRepositories.length === 0 ? (
          <div className="p-4 text-center text-sm text-slate-500 bg-slate-950/40 rounded-lg">
            No repositories found.
          </div>
        ) : (
          filteredRepositories.map((repo) => {
            const isSelected = repo.id === selectedRepositoryId;
            return (
              <button
                key={repo.id}
                type="button"
                onClick={() => onSelect(repo)}
                className={`w-full text-left p-3.5 rounded-lg border transition-all flex items-center justify-between ${
                  isSelected
                    ? 'bg-indigo-600/20 border-indigo-500 text-white'
                    : 'bg-slate-950/40 border-slate-800/80 text-slate-300 hover:bg-slate-800/50 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-md ${isSelected ? 'bg-indigo-600 text-white' : 'bg-slate-800 text-slate-400'}`}>
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                    </svg>
                  </div>
                  <div>
                    <div className="font-medium text-sm text-white flex items-center space-x-2">
                      <span>{repo.fullName}</span>
                      <span className={`px-2 py-0.5 text-[10px] rounded-full uppercase tracking-wider font-semibold ${
                        repo.private ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30' : 'bg-slate-800 text-slate-400'
                      }`}>
                        {repo.private ? 'Private' : 'Public'}
                      </span>
                    </div>
                    <span className="text-xs text-slate-400">Default branch: {repo.defaultBranch}</span>
                  </div>
                </div>
                {isSelected && (
                  <div className="text-indigo-400">
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                  </div>
                )}
              </button>
            );
          })
        )}
      </div>
    </div>
  );
};
