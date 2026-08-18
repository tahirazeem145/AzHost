import React, { useEffect, useState } from 'react';
import { githubService } from '../services/githubService';
import { GitHubBranch } from '../types/github';

interface GitHubBranchSelectorProps {
  repositoryId: number;
  selectedBranch?: string;
  defaultBranch?: string;
  onSelect: (branchName: string) => void;
}

export const GitHubBranchSelector: React.FC<GitHubBranchSelectorProps> = ({
  repositoryId,
  selectedBranch,
  defaultBranch,
  onSelect,
}) => {
  const [branches, setBranches] = useState<GitHubBranch[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchBranches = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await githubService.getBranches(repositoryId);
        setBranches(data);

        // Auto-select default branch if non selected
        if (!selectedBranch && data.length > 0) {
          const matchingDefault = data.find((b) => b.name === defaultBranch) || data[0];
          onSelect(matchingDefault.name);
        }
      } catch (err: any) {
        console.error('Failed to fetch branches:', err);
        setError(err.message || 'Failed to load repository branches.');
      } finally {
        setLoading(false);
      }
    };

    if (repositoryId) {
      fetchBranches();
    }
  }, [repositoryId]);

  if (loading) {
    return (
      <div className="space-y-2 animate-pulse">
        <div className="h-4 bg-slate-800 rounded w-1/4"></div>
        <div className="h-10 bg-slate-800 rounded w-full"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-3 bg-red-900/30 border border-red-500/40 rounded-lg text-red-300 text-xs">
        {error}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-slate-300">Select Production Branch</label>
      <select
        value={selectedBranch || ''}
        onChange={(e) => onSelect(e.target.value)}
        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-800 rounded-lg text-white focus:outline-none focus:border-indigo-500 text-sm"
      >
        {branches.map((b) => (
          <option key={b.name} value={b.name}>
            {b.name} {b.protected ? '🔒 (Protected)' : ''} {b.name === defaultBranch ? ' (Default)' : ''}
          </option>
        ))}
      </select>
    </div>
  );
};
