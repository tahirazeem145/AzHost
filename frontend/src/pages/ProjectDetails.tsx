import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { Project, UpdateProjectRequest } from '../types/project';
import { ProjectAnalysisResult, AnalysisUIStatus } from '../types/projectAnalysis';
import { ProjectBuild } from '../types/build';
import { ProjectDeployment } from '../types/deployment';
import { projectService } from '../services/projectService';
import { projectAnalysisService } from '../services/projectAnalysisService';
import { buildService } from '../services/buildService';
import { deploymentService } from '../services/deploymentService';
import { useNotification } from '../context/NotificationContext';
import { EditProjectModal } from '../components/EditProjectModal';
import { DeleteConfirmationModal } from '../components/DeleteConfirmationModal';
import { ProjectAnalysisCard } from '../components/ProjectAnalysisCard';
import { BuildStatusBadge } from '../components/BuildStatusBadge';
import { BuildLogViewer } from '../components/BuildLogViewer';
import { BuildHistoryList } from '../components/BuildHistoryList';
import { DeploymentProgress } from '../components/DeploymentProgress';
import { DeploymentUrlCard } from '../components/DeploymentUrlCard';
import { DeploymentHistoryList } from '../components/DeploymentHistoryList';
import { ArrowLeft, Edit3, Trash2, Sparkles, Info, Loader2, Hammer, Rocket, PackageCheck } from 'lucide-react';

export const ProjectDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useNotification();

  const [project, setProject] = useState<Project | null>(null);
  const [analysis, setAnalysis] = useState<ProjectAnalysisResult | null>(null);
  const [analysisStatus, setAnalysisStatus] = useState<AnalysisUIStatus>('NOT_ANALYZED');

  // Phase 4 Build Engine States
  const [builds, setBuilds] = useState<ProjectBuild[]>([]);
  const [activeBuild, setActiveBuild] = useState<ProjectBuild | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [isLogTruncated, setIsLogTruncated] = useState(false);
  const [isStartingBuild, setIsStartingBuild] = useState(false);

  // Phase 5 Deployment Engine States
  const [deployments, setDeployments] = useState<ProjectDeployment[]>([]);
  const [activeDeployment, setActiveDeployment] = useState<ProjectDeployment | null>(null);
  const [isDeploying, setIsDeploying] = useState(false);
  const [isRollingBack, setIsRollingBack] = useState(false);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const buildPollRef = useRef<any>(null);
  const deployPollRef = useRef<any>(null);

  const fetchProjectData = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const projData = await projectService.getProjectById(id);
      setProject(projData);

      // Fetch existing analysis
      try {
        const analysisData = await projectAnalysisService.getLatestAnalysis(id);
        if (analysisData) {
          setAnalysis(analysisData);
          setAnalysisStatus('SUCCESS');
        } else {
          setAnalysisStatus('NOT_ANALYZED');
        }
      } catch {
        setAnalysisStatus('NOT_ANALYZED');
      }

      // Fetch build history
      try {
        const buildList = await buildService.getBuilds(id);
        setBuilds(buildList);
        if (buildList.length > 0) {
          const latest = buildList[0];
          setActiveBuild(latest);
          fetchLogs(id, latest.id);
        }
      } catch {
        // Build history optional on initial load
      }

      // Fetch deployment history
      try {
        const depListRes = await deploymentService.getDeployments(id);
        setDeployments(depListRes.deployments || []);
        if (depListRes.deployments && depListRes.deployments.length > 0) {
          const activeDep = depListRes.deployments.find(d => d.active) || depListRes.deployments[0];
          setActiveDeployment(activeDep);
        }
      } catch {
        // Deployment history optional
      }

    } catch (err: any) {
      setError(err.message || 'Project not found');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchProjectData();
  }, [fetchProjectData]);

  const fetchLogs = async (projId: string, buildId: string) => {
    try {
      const logRes = await buildService.getBuildLogs(projId, buildId);
      setLogs(logRes.logs || []);
      setIsLogTruncated(logRes.truncated || false);
    } catch {
      // Ignore log fetch error
    }
  };

  // Live log polling when build is active
  useEffect(() => {
    if (!id || !activeBuild || activeBuild.status === 'SUCCESS' || activeBuild.status === 'FAILED' || activeBuild.status === 'CANCELLED' || activeBuild.status === 'TIMEOUT') {
      if (buildPollRef.current) clearInterval(buildPollRef.current);
      return;
    }

    buildPollRef.current = setInterval(async () => {
      try {
        const updatedBuild = await buildService.getBuildById(id, activeBuild.id);
        setActiveBuild(updatedBuild);
        fetchLogs(id, activeBuild.id);

        if (updatedBuild.status === 'SUCCESS' || updatedBuild.status === 'FAILED' || updatedBuild.status === 'CANCELLED' || updatedBuild.status === 'TIMEOUT') {
          if (buildPollRef.current) clearInterval(buildPollRef.current);
          const updatedHistory = await buildService.getBuilds(id);
          setBuilds(updatedHistory);
          if (updatedBuild.status === 'SUCCESS') {
            showToast('✓ Project build completed successfully!');
          }
        }
      } catch {
        if (buildPollRef.current) clearInterval(buildPollRef.current);
      }
    }, 1500);

    return () => {
      if (buildPollRef.current) clearInterval(buildPollRef.current);
    };
  }, [id, activeBuild]);

  // Live polling when deployment is active
  useEffect(() => {
    if (!id || !activeDeployment || activeDeployment.status === 'SUCCESS' || activeDeployment.status === 'FAILED' || activeDeployment.status === 'CANCELLED') {
      if (deployPollRef.current) clearInterval(deployPollRef.current);
      return;
    }

    deployPollRef.current = setInterval(async () => {
      try {
        const updatedDep = await deploymentService.getDeploymentById(id, activeDeployment.id);
        setActiveDeployment(updatedDep);

        if (updatedDep.status === 'SUCCESS' || updatedDep.status === 'FAILED' || updatedDep.status === 'CANCELLED') {
          if (deployPollRef.current) clearInterval(deployPollRef.current);
          const updatedListRes = await deploymentService.getDeployments(id);
          setDeployments(updatedListRes.deployments || []);
          if (updatedDep.status === 'SUCCESS') {
            showToast('✓ Artifact deployed successfully!');
          }
        }
      } catch {
        if (deployPollRef.current) clearInterval(deployPollRef.current);
      }
    }, 1500);

    return () => {
      if (deployPollRef.current) clearInterval(deployPollRef.current);
    };
  }, [id, activeDeployment]);

  const handleStartBuild = async () => {
    if (!id) return;
    setIsStartingBuild(true);
    try {
      const newBuild = await buildService.startBuild(id);
      setActiveBuild(newBuild);
      setLogs(['[AZHOST BUILD ENGINE] Build queued...']);
      const updatedHistory = await buildService.getBuilds(id);
      setBuilds(updatedHistory);
      showToast('✓ Build request submitted!');
    } catch (err: any) {
      showToast(err.message || 'Failed to start build', 'error');
    } finally {
      setIsStartingBuild(false);
    }
  };

  const handleDeployBuild = async (targetBuild: ProjectBuild) => {
    if (!id) return;
    setIsDeploying(true);
    try {
      const newDep = await deploymentService.createDeployment(id, targetBuild.id);
      setActiveDeployment(newDep);
      const updatedListRes = await deploymentService.getDeployments(id);
      setDeployments(updatedListRes.deployments || []);
      showToast('✓ Deployment request submitted!');
    } catch (err: any) {
      showToast(err.message || 'Failed to start deployment', 'error');
    } finally {
      setIsDeploying(false);
    }
  };

  const handleRollback = async (targetDep: ProjectDeployment) => {
    if (!id) return;
    setIsRollingBack(true);
    try {
      const updatedDep = await deploymentService.rollbackToDeployment(id, targetDep.id);
      setActiveDeployment(updatedDep);
      const updatedListRes = await deploymentService.getDeployments(id);
      setDeployments(updatedListRes.deployments || []);
      showToast(`✓ Rolled back to deployment ${targetDep.id.substring(0, 8)}...`);
    } catch (err: any) {
      showToast(err.message || 'Failed to rollback deployment', 'error');
    } finally {
      setIsRollingBack(false);
    }
  };

  const handleAnalyze = async () => {
    if (!id) return;
    setAnalysisStatus('ANALYZING');

    try {
      const result = await projectAnalysisService.analyzeProject(id);
      setAnalysis(result);
      setAnalysisStatus('SUCCESS');
      showToast('✓ Project analysis completed successfully.');
      const updatedProj = await projectService.getProjectById(id);
      setProject(updatedProj);
    } catch (err: any) {
      if (err.status === 409 || err.code === 'PROJECT_SOURCE_NOT_AVAILABLE') {
        setAnalysisStatus('SOURCE_UNAVAILABLE');
      } else {
        setAnalysisStatus('FAILED');
        showToast(err.message || 'Analysis failed', 'error');
      }
    }
  };

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

  const isBuildingActive = activeBuild ? ['QUEUED', 'PREPARING', 'INSTALLING', 'BUILDING'].includes(activeBuild.status) : false;
  const isDeployingActive = activeDeployment ? ['QUEUED', 'PREPARING', 'EXTRACTING', 'VALIDATING', 'PUBLISHING'].includes(activeDeployment.status) : false;

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
              onClick={handleStartBuild}
              disabled={isStartingBuild || isBuildingActive}
              className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-emerald-600/30 disabled:opacity-50"
            >
              <Hammer className={`w-4 h-4 ${isStartingBuild || isBuildingActive ? 'animate-bounce' : ''}`} />
              {isStartingBuild ? 'Starting...' : isBuildingActive ? 'Building...' : 'Build Project'}
            </button>

            {activeBuild && activeBuild.status === 'SUCCESS' && (
              <button
                onClick={() => handleDeployBuild(activeBuild)}
                disabled={isDeploying || isDeployingActive}
                className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/30 disabled:opacity-50"
              >
                <Rocket className={`w-4 h-4 ${isDeploying || isDeployingActive ? 'animate-bounce' : ''}`} />
                {isDeploying ? 'Deploying...' : isDeployingActive ? 'Publishing...' : 'Deploy Artifact'}
              </button>
            )}

            <button
              onClick={() => setIsEditOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold rounded-lg transition-colors border border-slate-700"
            >
              <Edit3 className="w-4 h-4" />
              Edit
            </button>

            <button
              onClick={() => setIsDeleteOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-rose-950/80 hover:bg-rose-900 text-rose-300 text-sm font-semibold rounded-lg transition-colors border border-rose-800/60"
            >
              <Trash2 className="w-4 h-4" />
              Delete
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
              <p className="text-slate-100 font-semibold">{project.framework}</p>
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
        </div>

        {/* LIVE DEPLOYED URL CARD (PHASE 5) */}
        {activeDeployment && activeDeployment.status === 'SUCCESS' && (
          <DeploymentUrlCard deployment={activeDeployment} />
        )}

        {/* ACTIVE DEPLOYMENT PROGRESS (PHASE 5) */}
        {activeDeployment && activeDeployment.status !== 'SUCCESS' && (
          <DeploymentProgress
            deployment={activeDeployment}
            onCancel={() => {
              if (id) deploymentService.cancelDeployment(id, activeDeployment.id);
            }}
          />
        )}

        {/* DEPLOYMENT HISTORY LIST (PHASE 5) */}
        {deployments.length > 0 && (
          <DeploymentHistoryList
            deployments={deployments}
            onRollback={handleRollback}
            isRollingBack={isRollingBack}
          />
        )}

        {/* ACTIVE BUILD & TERMINAL LOGS SECTION (PHASE 4) */}
        {activeBuild && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <h3 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
                  <Hammer className="w-5 h-5 text-emerald-400" />
                  Active Build Status
                </h3>
                <BuildStatusBadge status={activeBuild.status} />
              </div>

              {activeBuild.artifactId && (
                <span className="px-3 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-xs font-mono font-semibold flex items-center gap-1.5">
                  <PackageCheck className="w-4 h-4 text-emerald-400" />
                  Artifact ID: {activeBuild.artifactId}
                </span>
              )}
            </div>

            <BuildLogViewer logs={logs} isBuilding={isBuildingActive} truncated={isLogTruncated} />
          </div>
        )}

        {/* BUILD HISTORY SECTION (PHASE 4) */}
        {builds.length > 0 && (
          <BuildHistoryList
            builds={builds}
            onSelectBuild={(b) => {
              setActiveBuild(b);
              if (id) fetchLogs(id, b.id);
            }}
            selectedBuildId={activeBuild?.id}
          />
        )}

        {/* PROJECT ANALYSIS SECTION (PHASE 3) */}
        <div>
          {analysisStatus === 'SUCCESS' && analysis ? (
            <ProjectAnalysisCard
              analysis={analysis}
              onReanalyze={handleAnalyze}
              isAnalyzing={false}
            />
          ) : analysisStatus === 'ANALYZING' ? (
            <div className="glass-panel p-12 text-center flex flex-col items-center justify-center gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
              <h3 className="text-lg font-bold text-slate-200">Analyzing Project...</h3>
              <p className="text-xs text-slate-400">Safely inspecting project manifests and metadata</p>
            </div>
          ) : analysisStatus === 'SOURCE_UNAVAILABLE' ? (
            <div className="glass-panel p-8 space-y-4 border-slate-800/80">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-amber-950/80 border border-amber-800/80 flex items-center justify-center text-amber-400">
                    <Info className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-slate-200">Project Source Notice</h3>
                    <p className="text-xs text-amber-400 font-medium">
                      Project source is not available on disk yet.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="glass-panel p-8 flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-1">
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Sparkles className="w-5 h-5 text-blue-400" />
                  Project Analysis
                </h3>
                <p className="text-xs text-slate-400">
                  Project has not been analyzed yet. Run static inspection to detect framework, build tool, and dependencies.
                </p>
              </div>

              <button
                onClick={handleAnalyze}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold text-sm rounded-xl transition-all shadow-lg shadow-blue-600/30 hover:scale-[1.02] active:scale-[0.98]"
              >
                <Sparkles className="w-4 h-4" />
                Analyze Project
              </button>
            </div>
          )}
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
