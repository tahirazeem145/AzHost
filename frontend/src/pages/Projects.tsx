import React, { useEffect, useState, useCallback } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Project, CreateProjectRequest, UpdateProjectRequest } from '../types/project';
import { projectService } from '../services/projectService';
import { useNotification } from '../context/NotificationContext';
import { ProjectCard } from '../components/ProjectCard';
import { NewProjectModal } from '../components/NewProjectModal';
import { EditProjectModal } from '../components/EditProjectModal';
import { DeleteConfirmationModal } from '../components/DeleteConfirmationModal';
import { Search, Plus, FolderGit2, Loader2 } from 'lucide-react';

export const Projects: React.FC = () => {
  const { showToast } = useNotification();
  const [projects, setProjects] = useState<Project[]>([]);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modal states
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [deletingProject, setDeletingProject] = useState<Project | null>(null);

  const fetchProjects = useCallback(async (query?: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await projectService.getProjects(query);
      setProjects(data.projects);
      setTotalCount(data.totalCount);
    } catch (err: any) {
      setError(err.message || 'Failed to load projects');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  // Debounced Search Effect
  useEffect(() => {
    const timer = setTimeout(() => {
      fetchProjects(searchQuery);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery, fetchProjects]);

  const handleCreate = async (data: CreateProjectRequest) => {
    try {
      const created = await projectService.createProject(data);
      setProjects((prev) => [created, ...prev]);
      setTotalCount((prev) => prev + 1);
      showToast('✓ Project created successfully.');
    } catch (err: any) {
      showToast(err.message || 'Failed to create project', 'error');
      throw err;
    }
  };

  const handleUpdate = async (id: string, data: UpdateProjectRequest) => {
    try {
      const updated = await projectService.updateProject(id, data);
      setProjects((prev) => prev.map((p) => (p.id === id ? updated : p)));
      showToast('✓ Project updated successfully.');
    } catch (err: any) {
      showToast(err.message || 'Failed to update project', 'error');
      throw err;
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await projectService.deleteProject(id);
      setProjects((prev) => prev.filter((p) => p.id !== id));
      setTotalCount((prev) => Math.max(0, prev - 1));
      showToast('✓ Project deleted successfully.');
    } catch (err: any) {
      showToast(err.message || 'Failed to delete project', 'error');
      throw err;
    }
  };

  return (
    <DashboardLayout title="Projects">
      <div className="space-y-6">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-white tracking-tight">Your Projects</h2>
            <p className="text-slate-400 text-sm">Manage your hosted applications and deployment configurations.</p>
          </div>

          <button
            onClick={() => setIsNewModalOpen(true)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/30 hover:scale-[1.02] active:scale-[0.98]"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            New Project
          </button>
        </div>

        {/* Search Input Bar */}
        <div className="relative max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search projects by name or description..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors shadow-inner"
          />
        </div>

        {/* Content State Handling */}
        {isLoading && projects.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[300px] gap-3 text-slate-400">
            <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
            <p className="text-sm font-medium">Loading projects...</p>
          </div>
        ) : error ? (
          <div className="glass-panel p-8 text-center text-rose-400 text-sm font-medium">
            {error}
          </div>
        ) : projects.length === 0 ? (
          <div className="glass-panel p-12 text-center flex flex-col items-center justify-center">
            <div className="w-14 h-14 rounded-2xl bg-slate-800/60 border border-slate-700/60 flex items-center justify-center text-slate-400 mb-4">
              <FolderGit2 className="w-7 h-7 stroke-[1.5]" />
            </div>
            <h3 className="text-lg font-semibold text-slate-200 mb-1">
              {searchQuery ? 'No matching projects found' : 'No projects yet.'}
            </h3>
            <p className="text-sm text-slate-400 max-w-sm mb-6">
              {searchQuery
                ? `No projects matching "${searchQuery}". Try a different search term.`
                : 'Create your first project and manage it from AZHost.'}
            </p>
            <button
              onClick={() => setIsNewModalOpen(true)}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm rounded-xl transition-colors shadow-lg shadow-blue-600/30"
            >
              <Plus className="w-4 h-4" />
              Create Project
            </button>
          </div>
        ) : (
          <div>
            <div className="flex items-center justify-between text-xs text-slate-400 mb-4 font-medium">
              <span>Showing {projects.length} of {totalCount} projects</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {projects.map((project) => (
                <ProjectCard
                  key={project.id}
                  project={project}
                  onEdit={(p) => setEditingProject(p)}
                  onDelete={(p) => setDeletingProject(p)}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <NewProjectModal
        isOpen={isNewModalOpen}
        onClose={() => setIsNewModalOpen(false)}
        onSubmit={handleCreate}
      />

      <EditProjectModal
        project={editingProject}
        isOpen={!!editingProject}
        onClose={() => setEditingProject(null)}
        onSubmit={handleUpdate}
      />

      <DeleteConfirmationModal
        project={deletingProject}
        isOpen={!!deletingProject}
        onClose={() => setDeletingProject(null)}
        onConfirm={handleDelete}
      />
    </DashboardLayout>
  );
};
