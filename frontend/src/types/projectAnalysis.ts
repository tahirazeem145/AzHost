import { ProjectFramework } from './project';

export type DetectionConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

export interface ProjectAnalysisResult {
  projectId: string;
  framework: ProjectFramework;
  frameworkConfidence: DetectionConfidence;
  buildTool?: string;
  packageManager: string;
  packageManagerConfidence: DetectionConfidence;
  language: string;
  buildCommand?: string;
  devCommand?: string;
  outputDirectory: string;
  nodeVersion?: string;
  confidence: DetectionConfidence;
  executed: boolean;
  evidence: string[];
  warnings: string[];
  detectedFiles: string[];
  analyzedAt: string;
}

export type AnalysisUIStatus = 'NOT_ANALYZED' | 'ANALYZING' | 'SUCCESS' | 'FAILED' | 'SOURCE_UNAVAILABLE';
