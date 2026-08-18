import React from 'react';
import { ProjectBuild } from '../types/build';
import { BuildStatusBadge } from './BuildStatusBadge';
import { Clock, PackageCheck } from 'lucide-react';


interface BuildHistoryListProps {
  builds: ProjectBuild[];
  onSelectBuild?: (build: ProjectBuild) => void;
  selectedBuildId?: string;
}

export const BuildHistoryList: React.FC<BuildHistoryListProps> = ({
  builds,
  onSelectBuild,
  selectedBuildId,
}) => {
  const formatDate = (isoString: string) => {
    try {
      return new Date(isoString).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return 'N/A';
    if (ms < 1000) return `${ms} ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  };

  if (builds.length === 0) {
    return (
      <div className="glass-panel p-8 text-center text-slate-400 text-sm">
        No builds executed for this project yet.
      </div>
    );
  }

  return (
    <div className="glass-panel p-6 space-y-4">
      <h3 className="text-lg font-bold text-white flex items-center gap-2">
        <Clock className="w-5 h-5 text-blue-400" />
        Build History ({builds.length})
      </h3>

      <div className="divide-y divide-slate-800">
        {builds.map((build) => (
          <div
            key={build.id}
            onClick={() => onSelectBuild?.(build)}
            className={`p-4 rounded-xl transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-4 border ${
              selectedBuildId === build.id
                ? 'bg-blue-950/40 border-blue-800/80 shadow-lg'
                : 'bg-slate-950/50 border-slate-800/50 hover:bg-slate-900/60'
            }`}
          >
            <div className="flex items-center gap-4">
              <BuildStatusBadge status={build.status} />

              <div className="space-y-0.5">
                <span className="text-xs font-mono text-slate-400">ID: {build.id.substring(0, 8)}...</span>
                <p className="text-xs text-slate-300">
                  {build.packageManager} • Node {build.nodeVersion} • {build.buildCommand || 'npm run build'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-6 text-xs text-slate-400 font-mono">
              <div>
                <span className="text-[10px] text-slate-500 uppercase block">Duration</span>
                <span>{formatDuration(build.durationMs)}</span>
              </div>

              <div>
                <span className="text-[10px] text-slate-500 uppercase block">Created</span>
                <span>{formatDate(build.createdAt)}</span>
              </div>

              {build.artifactId && (
                <span className="px-2 py-1 rounded bg-emerald-950 text-emerald-400 border border-emerald-800/60 text-[11px] font-semibold flex items-center gap-1">
                  <PackageCheck className="w-3.5 h-3.5" />
                  Artifact Ready
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
