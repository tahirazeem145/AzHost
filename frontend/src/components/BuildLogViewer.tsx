import React, { useEffect, useRef, useState } from 'react';
import { Terminal, Download, ArrowDown } from 'lucide-react';

interface BuildLogViewerProps {
  logs: string[];
  isBuilding: boolean;
  truncated?: boolean;
}

export const BuildLogViewer: React.FC<BuildLogViewerProps> = ({ logs, isBuilding, truncated = false }) => {
  const logContainerRef = useRef<HTMLPreElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  useEffect(() => {
    if (autoScroll && logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  const downloadLogs = () => {
    const blob = new Blob([logs.join('\n')], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `build-log-${Date.now()}.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="bg-slate-950 rounded-xl border border-slate-800 overflow-hidden flex flex-col font-mono text-xs shadow-2xl">
      {/* Terminal Header Bar */}
      <div className="bg-slate-900/90 px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2 text-slate-400">
          <Terminal className="w-4 h-4 text-blue-400" />
          <span className="font-semibold text-slate-200">Build Execution Terminal Output</span>
          {isBuilding && (
            <span className="inline-flex items-center gap-1.5 text-[10px] text-amber-400 bg-amber-950/80 px-2 py-0.5 rounded border border-amber-800">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping" />
              Streaming...
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setAutoScroll(!autoScroll)}
            className={`px-2.5 py-1 rounded text-[11px] font-semibold border transition-colors flex items-center gap-1 ${
              autoScroll
                ? 'bg-blue-950 text-blue-400 border-blue-800'
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
          >
            <ArrowDown className="w-3 h-3" />
            Auto-scroll
          </button>

          <button
            onClick={downloadLogs}
            disabled={logs.length === 0}
            className="p-1.5 text-slate-400 hover:text-white transition-colors disabled:opacity-40"
            title="Download Logs"
          >
            <Download className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Terminal Content (Plain Text <pre>) */}
      <pre
        ref={logContainerRef}
        className="p-4 max-h-[450px] min-h-[220px] overflow-y-auto whitespace-pre-wrap text-slate-300 leading-relaxed font-mono select-text"
      >
        {logs.length === 0 ? (
          <span className="text-slate-600 italic">Waiting for log stream output...</span>
        ) : (
          logs.map((line, idx) => (
            <div key={idx} className="hover:bg-slate-900/50 px-1 rounded transition-colors">
              <span className="text-slate-600 text-[10px] inline-block w-8 select-none">{idx + 1}</span>
              <span>{line}</span>
            </div>
          ))
        )}
        {truncated && (
          <div className="mt-2 text-amber-400 italic bg-amber-950/40 p-2 rounded border border-amber-900">
            [AZHOST BUILD ENGINE] Note: Logs exceeded 10 MB limit and were truncated.
          </div>
        )}
      </pre>
    </div>
  );
};
