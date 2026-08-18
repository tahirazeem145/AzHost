import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Project, UpdateProjectRequest } from '../types/project';
import { projectService } from '../services/projectService';
import { useNotification } from '../context/NotificationContext';
import { EditProjectModal } from '../components/EditProjectModal';
import { DeleteConfirmationModal } from '../components/DeleteConfirmationModal';
import { ArrowLeft, Edit3, Trash2, Github, ExternalLink, Rocket, Globe, Key, Loader2 } from 'lucide-react';


export const ProjectDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useNotification();

  const [project, setProject] = useState<Project | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const fetchProject = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await projectService.getProjectById(id);
      setProject(data);
    } catch (err: any) {
      setError(err.message || 'Project not found');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchProject();
  }, [fetchProject]);

  const handleUpdate = async (projId: string, data: UpdateProjectRequest) => {
    try {
      const updated = await projectService.updateProject(projId, data);
      setProject(updated);
      showToast('✓ Project updated successfully.');
    } catch (err: any) {
      showToast(err.message || 'Failed to update project', 'error');
      throw err;
    }
  };

  const handleDelete = async (projId: string) => {
    try {
      await projectService.deleteProject(projId);
      showToast('✓ Project deleted successfully.');
      navigate('/projects');
    } catch (err: any) {
      showToast(err.message || 'Failed to delete project', 'error');
      throw err;
    }
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return 'N/A';
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

  if (isLoading) {
    return (
      <DashboardLayout title="Project Details">
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-3 text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
          <p className="text-sm font-medium">Loading project details...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !project) {
    return (
      <DashboardLayout title="Project Details">
        <div className="space-y-6">
          <Link
            to="/projects"
            className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Projects
          </Link>

          <div className="glass-panel p-12 text-center">
            <h3 className="text-lg font-bold text-rose-400 mb-2">Project Not Found</h3>
            <p className="text-slate-400 text-sm mb-6">{error || 'The requested project could not be found.'}</p>
            <Link
              to="/projects"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-lg transition-colors"
            >
              Return to Projects
            </Link>
          </div>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout title={project.name}>
      <div className="space-y-8">
        {/* Navigation Back Link + Action Controls */}
        <div className="flex items-center justify-between">
          <Link
            to="/projects"
            className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Projects
          </Link>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsEditOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold rounded-lg transition-colors border border-slate-700"
            >
              <Edit3 className="w-4 h-4" />
              Edit Project
            </button>

            <button
              onClick={() => setIsDeleteOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-rose-950/80 hover:bg-rose-900 text-rose-300 text-sm font-semibold rounded-lg transition-colors border border-rose-800/60"
            >
              <Trash2 className="w-4 h-4" />
              Delete Project
            </button>
          </div>
        </div>

        {/* Project Main Details Header Card */}
        <div className="glass-panel p-8 space-y-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-3xl font-bold text-white tracking-tight">{project.name}</h1>
                <span className="px-3 py-1 rounded-full bg-blue-950 text-blue-400 border border-blue-800/60 font-mono text-xs">
                  /{project.slug}
                </span>
                <span
                  className={`px-3 py-1 rounded-full text-xs font-bold border ${
                    project.status === 'ACTIVE'
                      ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                      : 'bg-slate-800 text-slate-400 border-slate-700'
                  }`}
                >
                  ● {project.status}
                </span>
              </div>
              <p className="text-slate-300 text-sm">{project.description || 'No description provided.'}</p>
            </div>
          </div>

          {/* Project Metadata Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
              <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Framework</span>
              <p className="text-slate-100 font-semibold flex items-center gap-2">
                <span>{project.framework}</span>
              </p>
            </div>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
              <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Source Type</span>
              <p className="text-slate-100 font-semibold">{project.sourceType}</p>
            </div>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
              <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Created</span>
              <p className="text-slate-300 text-xs font-mono">{formatDate(project.createdAt)}</p>
            </div>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 space-y-1">
              <span className="text-slate-500 text-xs font-semibold uppercase tracking-wider">Last Updated</span>
              <p className="text-slate-300 text-xs font-mono">{formatDate(project.updatedAt)}</p>
            </div>
          </div>

          {/* Repository Info if GitHub */}
          {project.sourceType === 'GITHUB' && project.repositoryUrl && (
            <div className="bg-slate-950 p-5 rounded-xl border border-slate-800/80 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <Github className="w-5 h-5 text-slate-300" />
                <div>
                  <span className="text-xs text-slate-500 font-medium">Connected Repository</span>
                  <p className="text-sm font-semibold text-slate-200">{project.repositoryUrl}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <span className="px-2.5 py-1 rounded bg-slate-900 text-slate-300 font-mono text-xs border border-slate-800">
                  Branch: {project.repositoryBranch || 'main'}
                </span>
                <a
                  href={project.repositoryUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="p-2 text-slate-400 hover:text-white transition-colors"
                >
                  <ExternalLink className="w-4 h-4" />
                </a>
              </div>
            </div>
          )}
        </div>

        {/* Future Feature Placeholders (Deployments, Domains, Environment Variables) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Deployments Placeholder */}
          <div className="glass-panel p-6 border-slate-800/60 opacity-75 relative overflow-hidden">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Rocket className="w-5 h-5 text-blue-400" />
                <h3 className="font-semibold text-slate-200 text-base">Deployments</h3>
              </div>
              <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800/50">
                Phase 5
              </span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed mb-4">
              Automated build logs, streaming deployment outputs, and instant rollbacks.
            </p>
            <span className="text-xs font-semibold text-slate-500 italic">Coming in a future phase</span>
          </div>

          {/* Custom Domains Placeholder */}
          <div className="glass-panel p-6 border-slate-800/60 opacity-75 relative overflow-hidden">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Globe className="w-5 h-5 text-blue-400" />
                <h3 className="font-semibold text-slate-200 text-base">Custom Domains</h3>
              </div>
              <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800/50">
                Phase 9
              </span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed mb-4">
              Custom CNAME routing and Let's Encrypt automated SSL certificates.
            </p>
            <span className="text-xs font-semibold text-slate-500 italic">Coming in a future phase</span>
          </div>

          {/* Environment Variables Placeholder */}
          <div className="glass-panel p-6 border-slate-800/60 opacity-75 relative overflow-hidden">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Key className="w-5 h-5 text-blue-400" />
                <h3 className="font-semibold text-slate-200 text-base">Environment Variables</h3>
              </div>
              <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800/50">
                Phase 3
              </span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed mb-4">
              Encrypted environment key-value pairs for production and preview builds.
            </p>
            <span className="text-xs font-semibold text-slate-500 italic">Coming in a future phase</span>
          </div>
        </div>
      </div>

      <EditProjectModal
        project={project}
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        onSubmit={handleUpdate}
      />

      <DeleteConfirmationModal
        project={project}
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDelete}
      />
    </DashboardLayout>
  );
};
