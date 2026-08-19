package com.azhost.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "azhost")
public class AzHostBuildProperties {

    private final Build build = new Build();
    private final Deployment deployment = new Deployment();
    private final RateLimit rateLimit = new RateLimit();

    public Build getBuild() {
        return build;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
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
}
