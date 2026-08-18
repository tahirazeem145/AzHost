import React from 'react';
import { Link } from 'react-router-dom';
import { Project } from '../types/project';
import { ExternalLink, Github, UploadCloud, Laptop, Edit3, Trash2 } from 'lucide-react';



interface ProjectCardProps {
  project: Project;
  onEdit?: (project: Project) => void;
  onDelete?: (project: Project) => void;
}

export const ProjectCard: React.FC<ProjectCardProps> = ({ project, onEdit, onDelete }) => {
  const getFrameworkIcon = (framework: string) => {
    switch (framework) {
      case 'REACT':
      case 'VITE':
        return '⚛️';
      case 'NEXT_JS':
        return '▲';
      case 'VUE':
        return '🟢';
      case 'ANGULAR':
        return '🅰️';
      case 'STATIC':
        return '📄';
      default:
        return '📦';
    }
  };

  const getSourceIcon = (source: string) => {
    switch (source) {
      case 'GITHUB':
        return <Github className="w-3.5 h-3.5 text-slate-300" />;
      case 'UPLOAD':
        return <UploadCloud className="w-3.5 h-3.5 text-slate-300" />;
      case 'LOCAL':
        return <Laptop className="w-3.5 h-3.5 text-slate-300" />;
      default:
        return null;
    }
  };

  const formatDate = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="glass-card p-6 flex flex-col justify-between group hover:border-slate-700 transition-all duration-200">
      <div>
        {/* Card Header: Icon + Name + Actions */}
        <div className="flex items-start justify-between gap-3 mb-3">
          <div className="flex items-center gap-3">
            <span className="text-2xl" role="img" aria-label="framework icon">
              {getFrameworkIcon(project.framework)}
            </span>
            <div>
              <Link
                to={`/projects/${project.id}`}
                className="font-bold text-lg text-white hover:text-blue-400 transition-colors tracking-tight line-clamp-1"
              >
                {project.name}
              </Link>
              <span className="text-xs text-slate-500 font-mono">/{project.slug}</span>
            </div>
          </div>

          <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
            {onEdit && (
              <button
                onClick={() => onEdit(project)}
                className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-md transition-colors"
                title="Edit Project"
              >
                <Edit3 className="w-4 h-4" />
              </button>
            )}
            {onDelete && (
              <button
                onClick={() => onDelete(project)}
                className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-md transition-colors"
                title="Delete Project"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Description */}
        <p className="text-slate-400 text-sm mb-6 line-clamp-2 min-h-[2.5rem]">
          {project.description || 'No description provided.'}
        </p>

        {/* Badges */}
        <div className="flex flex-wrap items-center gap-2 mb-6 text-xs">
          <span className="px-2.5 py-1 rounded-md bg-slate-800/80 text-slate-300 font-medium border border-slate-700/50">
            {project.framework}
          </span>

          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-slate-800/80 text-slate-300 font-medium border border-slate-700/50">
            {getSourceIcon(project.sourceType)}
            {project.sourceType}
          </span>

          <span
            className={`px-2.5 py-1 rounded-md font-semibold border ${
              project.status === 'ACTIVE'
                ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800/60'
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
          >
            ● {project.status}
          </span>
        </div>
      </div>

      {/* Footer: Date + Open Button */}
      <div className="pt-4 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-500">
        <span className="font-medium">Created {formatDate(project.createdAt)}</span>

        <Link
          to={`/projects/${project.id}`}
          className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-950/80 hover:bg-blue-900 text-blue-400 hover:text-blue-300 rounded-lg font-semibold border border-blue-800/60 transition-colors"
        >
          Open
          <ExternalLink className="w-3.5 h-3.5" />
        </Link>
      </div>
    </div>
  );
};
