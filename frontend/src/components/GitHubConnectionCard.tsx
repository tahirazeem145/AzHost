import React, { useEffect, useState } from 'react';
import { githubService } from '../services/githubService';
import { GitHubConnection } from '../types/github';

interface GitHubConnectionCardProps {
  onConnectionChange?: (connection: GitHubConnection) => void;
}

export const GitHubConnectionCard: React.FC<GitHubConnectionCardProps> = ({ onConnectionChange }) => {
  const [connection, setConnection] = useState<GitHubConnection | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchConnection = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await githubService.getConnection();
      setConnection(data);
      if (onConnectionChange) {
        onConnectionChange(data);
      }
    } catch (err) {
      console.error('Failed to fetch GitHub connection:', err);
      setError('Failed to check GitHub connection status.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConnection();
  }, []);

  const handleConnect = async () => {
    try {
      setLoading(true);
      const url = await githubService.getConnectUrl();
      window.location.href = url;
    } catch (err) {
      console.error('Failed to initiate GitHub OAuth connect:', err);
      setError('Could not connect to GitHub OAuth.');
      setLoading(false);
    }
  };

  const handleDisconnect = async () => {
    if (!window.confirm('Are you sure you want to disconnect your GitHub account?')) {
      return;
    }
    try {
      setLoading(true);
      await githubService.disconnect();
      await fetchConnection();
    } catch (err) {
      console.error('Failed to disconnect GitHub account:', err);
      setError('Failed to disconnect GitHub account.');
      setLoading(false);
    }
  };

  if (loading && !connection) {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl animate-pulse">
        <div className="h-5 bg-slate-800 rounded w-1/3 mb-4"></div>
        <div className="h-10 bg-slate-800 rounded w-full"></div>
      </div>
    );
  }

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
            <h3 className="text-lg font-semibold">GitHub Integration</h3>
            <p className="text-sm text-slate-400">Connect your account to deploy directly from GitHub repositories.</p>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-900/40 border border-red-500/50 rounded-lg text-red-300 text-sm">
          {error}
        </div>
      )}

      {connection && connection.connected ? (
        <div className="flex items-center justify-between p-4 bg-slate-800/60 border border-slate-700/60 rounded-xl mt-4">
          <div className="flex items-center space-x-3">
            {connection.avatarUrl ? (
              <img src={connection.avatarUrl} alt={connection.githubUsername} className="w-10 h-10 rounded-full border border-indigo-500/40" />
            ) : (
              <div className="w-10 h-10 rounded-full bg-indigo-600 flex items-center justify-center font-bold">
                {connection.githubUsername?.charAt(0).toUpperCase()}
              </div>
            )}
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-medium text-white">{connection.githubUsername}</span>
                <span className="px-2 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-full font-medium">✓ Connected</span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Connected on {connection.connectedAt ? new Date(connection.connectedAt).toLocaleDateString() : 'N/A'}
              </p>
            </div>
          </div>
          <button
            onClick={handleDisconnect}
            disabled={loading}
            className="px-4 py-2 text-sm text-slate-300 hover:text-red-400 hover:bg-red-500/10 border border-slate-700 hover:border-red-500/30 rounded-lg transition-all"
          >
            Disconnect
          </button>
        </div>
      ) : (
        <div className="mt-4 p-4 bg-slate-800/40 border border-slate-800 rounded-xl flex items-center justify-between">
          <p className="text-sm text-slate-400">No GitHub account linked to your AZHost profile.</p>
          <button
            onClick={handleConnect}
            disabled={loading}
            className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-lg shadow-lg shadow-indigo-600/30 transition-all flex items-center space-x-2"
          >
            <span>Connect GitHub</span>
          </button>
        </div>
      )}
    </div>
  );
};
