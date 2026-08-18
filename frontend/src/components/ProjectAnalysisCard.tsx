import React, { useState } from 'react';
import { ProjectAnalysisResult } from '../types/projectAnalysis';
import { DetectionBadge } from './DetectionBadge';
import { DetectionWarning } from './DetectionWarning';
import { Sparkles, ChevronDown, ChevronUp, FileCode, CheckCircle2, ShieldCheck, Info } from 'lucide-react';


interface ProjectAnalysisCardProps {
  analysis: ProjectAnalysisResult;
  onReanalyze?: () => void;
  isAnalyzing?: boolean;
}

export const ProjectAnalysisCard: React.FC<ProjectAnalysisCardProps> = ({
  analysis,
  onReanalyze,
  isAnalyzing = false,
}) => {
  const [showEvidence, setShowEvidence] = useState(false);

  const formatDate = (isoString: string) => {
    try {
      return new Date(isoString).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="glass-panel p-6 space-y-6 relative overflow-hidden">
      {/* Card Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-950/80 border border-blue-800/80 flex items-center justify-center text-blue-400">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
              Project Analysis
              <DetectionBadge label={analysis.confidence} confidence={analysis.confidence} />
            </h3>
            <p className="text-xs text-slate-400">
              Analyzed {formatDate(analysis.analyzedAt)} • Static inspection (Zero execution)
            </p>
          </div>
        </div>

        {onReanalyze && (
          <button
            onClick={onReanalyze}
            disabled={isAnalyzing}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-950/80 hover:bg-blue-900 text-blue-300 font-semibold text-xs rounded-xl border border-blue-800/60 transition-colors disabled:opacity-50"
          >
            <Sparkles className={`w-3.5 h-3.5 ${isAnalyzing ? 'animate-spin' : ''}`} />
            {isAnalyzing ? 'Analyzing...' : 'Re-analyze Project'}
          </button>
        )}
      </div>

      {/* Warnings List if any */}
      {analysis.warnings && analysis.warnings.length > 0 && (
        <div className="space-y-2">
          <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Analysis Warnings</span>
          <div className="space-y-2">
            {analysis.warnings.map((warning, idx) => (
              <DetectionWarning key={idx} warning={warning} />
            ))}
          </div>
        </div>
      )}

      {/* Main Analysis Metadata Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
        {/* Framework */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
          <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Framework</span>
          <p className="text-slate-100 font-bold text-base flex items-center justify-between">
            <span>{analysis.framework}</span>
            <span className="text-xs text-slate-400 font-normal">{analysis.buildTool}</span>
          </p>
        </div>

        {/* Package Manager */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1 group relative">
          <div className="flex items-center justify-between">
            <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Package Manager</span>
            <Info className="w-3.5 h-3.5 text-slate-500 cursor-help" />
          </div>
          <p className="text-slate-100 font-bold text-base">{analysis.packageManager}</p>
        </div>

        {/* Node Version */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
          <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Node.js Version</span>
          <p className="text-slate-200 font-mono text-sm">{analysis.nodeVersion || 'Not declared'}</p>
        </div>

        {/* Language */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
          <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Language</span>
          <p className="text-slate-200 font-semibold text-sm">{analysis.language}</p>
        </div>

        {/* Build Command */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1 col-span-1 sm:col-span-2">
          <div className="flex items-center justify-between">
            <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Build Command</span>
            <span className="text-[10px] text-slate-500 font-mono">Not executed</span>
          </div>
          <p className="text-emerald-400 font-mono text-sm bg-slate-900 px-3 py-1.5 rounded border border-slate-800 inline-block w-full">
            {analysis.buildCommand || 'Not detected'}
          </p>
          <p className="text-[11px] text-slate-400 italic">
            The command AZHost expects to use later to create your production files.
          </p>
        </div>

        {/* Dev Command */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1 col-span-1">
          <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Dev Command</span>
          <p className="text-blue-400 font-mono text-xs bg-slate-900 px-3 py-1.5 rounded border border-slate-800 block truncate">
            {analysis.devCommand || 'Not detected'}
          </p>
        </div>

        {/* Output Directory */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1 col-span-1">
          <div className="flex items-center justify-between">
            <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Output Directory</span>
            <Info className="w-3.5 h-3.5 text-slate-500 cursor-help" />
          </div>
          <p className="text-slate-200 font-mono text-sm bg-slate-900 px-3 py-1.5 rounded border border-slate-800 inline-block">
            {analysis.outputDirectory}
          </p>
          <p className="text-[11px] text-slate-400 italic">
            The folder where built static assets are placed.
          </p>
        </div>
      </div>

      {/* Security Statement Banner */}
      <div className="bg-slate-950/80 p-3 rounded-xl border border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Security Guarantee: 100% metadata inspection. Zero code or script execution.</span>
        </div>
        <span className="text-[10px] font-mono text-slate-500">executed: false</span>
      </div>

      {/* Expandable Evidence Details */}
      {analysis.evidence && analysis.evidence.length > 0 && (
        <div className="border-t border-slate-800/80 pt-4">
          <button
            onClick={() => setShowEvidence(!showEvidence)}
            className="flex items-center justify-between w-full text-xs font-semibold text-slate-400 hover:text-slate-200 transition-colors"
          >
            <span className="flex items-center gap-2">
              <FileCode className="w-4 h-4 text-blue-400" />
              Evidence Details ({analysis.evidence.length} indicators found)
            </span>
            {showEvidence ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showEvidence && (
            <div className="mt-3 p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-2 animate-fadeIn">
              <ul className="space-y-1.5 text-xs text-slate-300">
                {analysis.evidence.map((item, idx) => (
                  <li key={idx} className="flex items-center gap-2 font-mono">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>

              {analysis.detectedFiles && analysis.detectedFiles.length > 0 && (
                <div className="pt-3 border-t border-slate-900">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-1.5">
                    Scanned Manifest Files:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {analysis.detectedFiles.map((file, idx) => (
                      <span
                        key={idx}
                        className="px-2 py-0.5 rounded bg-slate-900 text-slate-400 font-mono text-[11px] border border-slate-800"
                      >
                        {file}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
