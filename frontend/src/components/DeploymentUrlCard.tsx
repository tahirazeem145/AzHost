import React, { useState } from 'react';
import { ProjectDeployment } from '../types/deployment';
import { Globe, ExternalLink, Copy, Check } from 'lucide-react';

interface DeploymentUrlCardProps {
  deployment: ProjectDeployment;
}

export const DeploymentUrlCard: React.FC<DeploymentUrlCardProps> = ({ deployment }) => {
  const [copied, setCopied] = useState(false);

  if (!deployment.deploymentUrl || deployment.status !== 'SUCCESS') return null;

  const handleCopy = () => {
    if (deployment.deploymentUrl) {
      navigator.clipboard.writeText(deployment.deploymentUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="glass-panel p-6 bg-gradient-to-br from-slate-900 via-slate-950 to-blue-950/40 border-emerald-800/40 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-xl bg-emerald-950/80 border border-emerald-800 flex items-center justify-center text-emerald-400 shadow-lg shadow-emerald-950/50">
            <Globe className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-bold text-white tracking-tight">Active Production Deployment</h3>
              <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold uppercase tracking-wider border border-emerald-500/30">
                Live
              </span>
            </div>
            <p className="text-xs text-slate-400">Static site published & served via AZHost static engine</p>
          </div>
        </div>

        <a
          href={deployment.deploymentUrl}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-emerald-600/30 hover:scale-[1.02] active:scale-[0.98]"
        >
          <ExternalLink className="w-4 h-4" />
          Open Live Site
        </a>
      </div>

      <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex items-center justify-between gap-3">
        <span className="text-xs font-mono text-emerald-400 truncate select-all">{deployment.deploymentUrl}</span>
        <button
          onClick={handleCopy}
          className="p-2 text-slate-400 hover:text-white transition-colors rounded-lg hover:bg-slate-900 flex-shrink-0"
          title="Copy URL"
        >
          {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
        </button>
      </div>
    </div>
  );
};
