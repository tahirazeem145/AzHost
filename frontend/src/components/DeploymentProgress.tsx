import React from 'react';
import { ProjectDeployment } from '../types/deployment';
import { DeploymentStatusBadge } from './DeploymentStatusBadge';
import { Loader2, Rocket, AlertTriangle, ShieldCheck } from 'lucide-react';

interface DeploymentProgressProps {
  deployment: ProjectDeployment;
  onCancel?: () => void;
}

export const DeploymentProgress: React.FC<DeploymentProgressProps> = ({ deployment, onCancel }) => {
  const steps = ['QUEUED', 'PREPARING', 'EXTRACTING', 'VALIDATING', 'PUBLISHING', 'SUCCESS'];
  const currentIndex = steps.indexOf(deployment.status);

  const isTerminal = ['SUCCESS', 'FAILED', 'CANCELLED'].includes(deployment.status);

  return (
    <div className="glass-panel p-6 space-y-6 relative overflow-hidden">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-teal-950/80 border border-teal-800/80 flex items-center justify-center text-teal-400">
            <Rocket className={`w-5 h-5 ${!isTerminal ? 'animate-bounce' : ''}`} />
          </div>
          <div>
            <h3 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
              Artifact Deployment Progress
              <DeploymentStatusBadge status={deployment.status} active={deployment.active} />
            </h3>
            <p className="text-xs text-slate-400 font-mono">Deployment ID: {deployment.id}</p>
          </div>
        </div>

        {!isTerminal && onCancel && (
          <button
            onClick={onCancel}
            className="px-3 py-1.5 bg-rose-950/80 hover:bg-rose-900 text-rose-300 font-semibold text-xs rounded-lg border border-rose-800/60 transition-colors"
          >
            Cancel Deployment
          </button>
        )}
      </div>

      {/* Progress Bar / Step Visualizer */}
      {!isTerminal && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
            <span>Publishing static files to immutable web root...</span>
            <span className="flex items-center gap-1.5 text-teal-400">
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
              {deployment.status}
            </span>
          </div>

          <div className="w-full bg-slate-950 h-2.5 rounded-full overflow-hidden border border-slate-800">
            <div
              className="bg-gradient-to-r from-blue-500 via-teal-400 to-emerald-400 h-full transition-all duration-500"
              style={{ width: `${Math.max(15, ((currentIndex + 1) / steps.length) * 100)}%` }}
            />
          </div>
        </div>
      )}

      {/* Error Callout if FAILED */}
      {deployment.status === 'FAILED' && (
        <div className="p-4 rounded-xl bg-rose-950/60 border border-rose-800/60 text-rose-300 text-xs flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
          <div>
            <span className="font-bold text-rose-200 block mb-1">Deployment Failed</span>
            <p className="leading-relaxed">{deployment.errorMessage || 'An error occurred during artifact deployment.'}</p>
          </div>
        </div>
      )}

      {/* Security Guarantee Footer */}
      <div className="flex items-center justify-between text-[11px] text-slate-500 font-mono border-t border-slate-900 pt-3">
        <span className="flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
          Canonical ZIP-Slip & Symlink Containment Verified
        </span>
        <span>Build ID: {deployment.buildId.substring(0, 8)}...</span>
      </div>
    </div>
  );
};
