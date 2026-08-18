import React from 'react';
import { ProjectDeployment } from '../types/deployment';
import { DeploymentStatusBadge } from './DeploymentStatusBadge';
import { Rocket, ExternalLink, RotateCcw } from 'lucide-react';

interface DeploymentHistoryListProps {
  deployments: ProjectDeployment[];
  onRollback?: (deployment: ProjectDeployment) => void;
  isRollingBack?: boolean;
}

export const DeploymentHistoryList: React.FC<DeploymentHistoryListProps> = ({
  deployments,
  onRollback,
  isRollingBack = false,
}) => {
  const formatDate = (isoString?: string) => {
    if (!isoString) return 'N/A';
    try {
      return new Date(isoString).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  if (deployments.length === 0) {
    return (
      <div className="glass-panel p-8 text-center text-slate-400 text-sm">
        No deployments created for this project yet.
      </div>
    );
  }

  return (
    <div className="glass-panel p-6 space-y-4">
      <h3 className="text-lg font-bold text-white flex items-center gap-2">
        <Rocket className="w-5 h-5 text-teal-400" />
        Deployment History ({deployments.length})
      </h3>

      <div className="divide-y divide-slate-800">
        {deployments.map((dep) => (
          <div
            key={dep.id}
            className={`p-4 rounded-xl transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 border ${
              dep.active
                ? 'bg-emerald-950/30 border-emerald-800/80 shadow-lg'
                : 'bg-slate-950/50 border-slate-800/50 hover:bg-slate-900/60'
            }`}
          >
            <div className="flex items-center gap-4">
              <DeploymentStatusBadge status={dep.status} active={dep.active} />

              <div className="space-y-0.5">
                <span className="text-xs font-mono text-slate-400">Deployment ID: {dep.id.substring(0, 8)}...</span>
                <p className="text-xs text-slate-300">
                  Build ID: <span className="font-mono text-slate-400">{dep.buildId.substring(0, 8)}...</span> • Artifact: <span className="font-mono text-slate-400">{dep.artifactId}</span>
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 text-xs text-slate-400 font-mono">
              <div>
                <span className="text-[10px] text-slate-500 uppercase block">Created</span>
                <span>{formatDate(dep.createdAt)}</span>
              </div>

              {dep.deploymentUrl && dep.status === 'SUCCESS' && (
                <a
                  href={dep.deploymentUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-slate-200 rounded-lg border border-slate-800 transition-colors flex items-center gap-1.5"
                >
                  <ExternalLink className="w-3.5 h-3.5 text-emerald-400" />
                  View
                </a>
              )}

              {!dep.active && dep.status === 'SUCCESS' && onRollback && (
                <button
                  onClick={() => onRollback(dep)}
                  disabled={isRollingBack}
                  className="px-3 py-1.5 bg-blue-950 hover:bg-blue-900 text-blue-300 rounded-lg border border-blue-800/80 transition-colors flex items-center gap-1.5 disabled:opacity-50"
                  title="Make this deployment active"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  Rollback
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
