import React from 'react';
import { DeploymentStatus } from '../types/deployment';
import { Loader2, CheckCircle2, XCircle, Clock, Sparkles } from 'lucide-react';

interface DeploymentStatusBadgeProps {
  status: DeploymentStatus;
  active?: boolean;
}

export const DeploymentStatusBadge: React.FC<DeploymentStatusBadgeProps> = ({ status, active = false }) => {
  const badgeConfig = {
    QUEUED: { style: 'bg-slate-800 text-slate-300 border-slate-700', icon: Clock, animate: false },
    PREPARING: { style: 'bg-blue-950 text-blue-300 border-blue-800', icon: Loader2, animate: true },
    EXTRACTING: { style: 'bg-indigo-950 text-indigo-300 border-indigo-800', icon: Loader2, animate: true },
    VALIDATING: { style: 'bg-purple-950 text-purple-300 border-purple-800', icon: Loader2, animate: true },
    PUBLISHING: { style: 'bg-teal-950 text-teal-300 border-teal-800', icon: Loader2, animate: true },
    SUCCESS: { style: 'bg-emerald-950 text-emerald-300 border-emerald-800', icon: CheckCircle2, animate: false },
    FAILED: { style: 'bg-rose-950 text-rose-300 border-rose-800', icon: XCircle, animate: false },
    CANCELLED: { style: 'bg-slate-900 text-slate-400 border-slate-800', icon: XCircle, animate: false },
  }[status];

  const Icon = badgeConfig.icon;

  return (
    <div className="inline-flex items-center gap-2">
      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold border font-mono ${badgeConfig.style}`}>
        <Icon className={`w-3.5 h-3.5 ${badgeConfig.animate ? 'animate-spin' : ''}`} />
        <span>{status}</span>
      </span>

      {active && (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-[10px] font-bold uppercase tracking-wider">
          <Sparkles className="w-3 h-3 text-emerald-400" />
          Active Production
        </span>
      )}
    </div>
  );
};
