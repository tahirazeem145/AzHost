import React from 'react';
import { useBackendStatus } from '../context/BackendStatusContext';
import { RefreshCw } from 'lucide-react';

interface HeaderProps {
  title: string;
}

export const Header: React.FC<HeaderProps> = ({ title }) => {
  const { isConnected, isLoading, checkStatus } = useBackendStatus();

  return (
    <header className="h-16 border-b border-slate-800 bg-slate-900/40 backdrop-blur-sm px-8 flex items-center justify-between sticky top-0 z-10">
      <h2 className="text-xl font-semibold text-slate-100">{title}</h2>

      <div className="flex items-center gap-4">
        {/* Backend Connectivity Status Indicator */}
        <div className="flex items-center gap-3 bg-slate-950/80 px-3.5 py-1.5 rounded-full border border-slate-800 text-xs">
          <span className="text-slate-400 font-medium">Backend</span>
          <div className="flex items-center gap-1.5 font-semibold">
            {isLoading ? (
              <>
                <span className="w-2 h-2 rounded-full bg-amber-400 animate-ping" />
                <span className="text-amber-400">Connecting...</span>
              </>
            ) : isConnected ? (
              <>
                <span className="w-2 h-2 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500" />
                <span className="text-emerald-400">Connected</span>
              </>
            ) : (
              <>
                <span className="w-2 h-2 rounded-full bg-rose-500 shadow-sm shadow-rose-500" />
                <span className="text-rose-400">Offline</span>
              </>
            )}
          </div>
          <button
            onClick={checkStatus}
            title="Refresh status"
            className="text-slate-400 hover:text-slate-200 transition-colors ml-1"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>
    </header>
  );
};
