import React from 'react';
import { DetectionConfidence } from '../types/projectAnalysis';

interface DetectionBadgeProps {
  label: string;
  confidence?: DetectionConfidence;
  type?: 'framework' | 'packageManager' | 'confidence';
}

export const DetectionBadge: React.FC<DetectionBadgeProps> = ({ label, confidence = 'HIGH' }) => {
  const confidenceStyles = {
    HIGH: 'bg-emerald-950/80 text-emerald-300 border-emerald-800/80',
    MEDIUM: 'bg-amber-950/80 text-amber-300 border-amber-800/80',
    LOW: 'bg-slate-800 text-slate-400 border-slate-700',
  }[confidence];

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold border font-mono ${confidenceStyles}`}
    >
      <span>●</span>
      <span>{label}</span>
      <span className="opacity-60 text-[10px]">({confidence})</span>
    </span>
  );
};
