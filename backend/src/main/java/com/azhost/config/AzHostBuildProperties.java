package com.azhost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azhost")
public class AzHostBuildProperties {

    private final Build build = new Build();
    private final Deployment deployment = new Deployment();
    private final RateLimit rateLimit = new RateLimit();
    private final Limits limits = new Limits();

    public Build getBuild() {
        return build;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Limits getLimits() {
        return limits;
    }

    public static class Build {
        private int maxConcurrentBuilds = 2;
        private int queueCapacity = 50;
        private long timeoutSeconds = 600;
        private int maxRetries = 2;
        private String cpuLimit = "2.0";
        private String memoryLimit = "2048m";
        private int pidsLimit = 100;

        public int getMaxConcurrentBuilds() {
            return maxConcurrentBuilds;
        }

        public void setMaxConcurrentBuilds(int maxConcurrentBuilds) {
            this.maxConcurrentBuilds = maxConcurrentBuilds;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public String getCpuLimit() {
            return cpuLimit;
        }

        public void setCpuLimit(String cpuLimit) {
            this.cpuLimit = cpuLimit;
        }

        public String getMemoryLimit() {
            return memoryLimit;
        }

        public void setMemoryLimit(String memoryLimit) {
            this.memoryLimit = memoryLimit;
        }

        public int getPidsLimit() {
            return pidsLimit;
        }

        public void setPidsLimit(int pidsLimit) {
            this.pidsLimit = pidsLimit;
        }
    }

    public static class Deployment {
        private long timeoutSeconds = 300;
        private final Retention retention = new Retention();

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public Retention getRetention() {
            return retention;
        }

        public static class Retention {
            private int maxDeploymentsPerProject = 20;

            public int getMaxDeploymentsPerProject() {
                return maxDeploymentsPerProject;
            }

            public void setMaxDeploymentsPerProject(int maxDeploymentsPerProject) {
                this.maxDeploymentsPerProject = maxDeploymentsPerProject;
            }
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    public static class Limits {
        private final PerProject perProject = new PerProject();
        private final PerUser perUser = new PerUser();

        public PerProject getPerProject() {
            return perProject;
        }

        public PerUser getPerUser() {
            return perUser;
        }

        public static class PerProject {
            private int maxConcurrentBuilds = 1;
            private int maxQueuedBuilds = 5;
            private long maxArtifactStorageBytes = 500 * 1024 * 1024L; // 500 MB

            public int getMaxConcurrentBuilds() {
                return maxConcurrentBuilds;
            }

            public void setMaxConcurrentBuilds(int maxConcurrentBuilds) {
                this.maxConcurrentBuilds = maxConcurrentBuilds;
            }

            public int getMaxQueuedBuilds() {
                return maxQueuedBuilds;
            }

            public void setMaxQueuedBuilds(int maxQueuedBuilds) {
                this.maxQueuedBuilds = maxQueuedBuilds;
            }

            public long getMaxArtifactStorageBytes() {
                return maxArtifactStorageBytes;
            }

            public void setMaxArtifactStorageBytes(long maxArtifactStorageBytes) {
                this.maxArtifactStorageBytes = maxArtifactStorageBytes;
            }
        }

        public static class PerUser {
            private int maxConcurrentBuilds = 2;
            private int maxQueuedBuilds = 10;

            public int getMaxConcurrentBuilds() {
                return maxConcurrentBuilds;
            }

            public void setMaxConcurrentBuilds(int maxConcurrentBuilds) {
                this.maxConcurrentBuilds = maxConcurrentBuilds;
            }

            public int getMaxQueuedBuilds() {
                return maxQueuedBuilds;
            }

            public void setMaxQueuedBuilds(int maxQueuedBuilds) {
                this.maxQueuedBuilds = maxQueuedBuilds;
            }
        }
    }
}
