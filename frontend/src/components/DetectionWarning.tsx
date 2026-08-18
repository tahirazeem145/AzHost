import React from 'react';
import { AlertTriangle } from 'lucide-react';

interface DetectionWarningProps {
  warning: string;
}

export const DetectionWarning: React.FC<DetectionWarningProps> = ({ warning }) => {
  return (
    <div className="flex items-start gap-2.5 p-3 rounded-lg bg-amber-950/60 border border-amber-800/60 text-amber-300 text-xs font-medium">
      <AlertTriangle className="w-4 h-4 flex-shrink-0 text-amber-400 mt-0.5" />
      <span className="leading-relaxed">{warning}</span>
    </div>
  );
};
