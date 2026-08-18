import React from 'react';
import { FolderGit2, Rocket, Globe, CheckCircle2, LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: number | string;
  iconName: 'projects' | 'deployments' | 'live-sites' | 'successful-deployments';
  subtitle?: string;
}

const iconMap: Record<string, LucideIcon> = {
  projects: FolderGit2,
  deployments: Rocket,
  'live-sites': Globe,
  'successful-deployments': CheckCircle2,
};

export const StatCard: React.FC<StatCardProps> = ({ title, value, iconName, subtitle }) => {
  const Icon = iconMap[iconName] || FolderGit2;

  return (
    <div className="glass-card p-6 flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-slate-400">{title}</span>
        <div className="p-2.5 rounded-lg bg-blue-950/60 border border-blue-800/40 text-blue-400">
          <Icon className="w-5 h-5" />
        </div>
      </div>
      <div className="mt-4">
        <div className="text-3xl font-bold text-slate-100 tracking-tight">{value}</div>
        {subtitle && <p className="text-xs text-slate-500 mt-1 font-medium">{subtitle}</p>}
      </div>
    </div>
  );
};
