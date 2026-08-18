import React from 'react';
import { BuildStatus } from '../types/build';
import { Loader2, CheckCircle2, XCircle, Clock, AlertOctagon } from 'lucide-react';

interface BuildStatusBadgeProps {
  status: BuildStatus;
}

export const BuildStatusBadge: React.FC<BuildStatusBadgeProps> = ({ status }) => {
  const badgeConfig = {
    QUEUED: { style: 'bg-slate-800 text-slate-300 border-slate-700', icon: Clock, animate: false },
    PREPARING: { style: 'bg-blue-950 text-blue-300 border-blue-800', icon: Loader2, animate: true },
    INSTALLING: { style: 'bg-indigo-950 text-indigo-300 border-indigo-800', icon: Loader2, animate: true },
    BUILDING: { style: 'bg-amber-950 text-amber-300 border-amber-800', icon: Loader2, animate: true },
    SUCCESS: { style: 'bg-emerald-950 text-emerald-300 border-emerald-800', icon: CheckCircle2, animate: false },
    FAILED: { style: 'bg-rose-950 text-rose-300 border-rose-800', icon: XCircle, animate: false },
    TIMEOUT: { style: 'bg-orange-950 text-orange-300 border-orange-800', icon: AlertOctagon, animate: false },
    CANCELLED: { style: 'bg-slate-900 text-slate-400 border-slate-800', icon: XCircle, animate: false },
  }[status];

  const Icon = badgeConfig.icon;

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold border font-mono ${badgeConfig.style}`}>
      <Icon className={`w-3.5 h-3.5 ${badgeConfig.animate ? 'animate-spin' : ''}`} />
      <span>{status}</span>
    </span>
  );
};
