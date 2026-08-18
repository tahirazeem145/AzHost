package com.azhost.analysis.detector;

import com.azhost.analysis.source.ProjectSourceReader;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BuildCommandDetector {

    private static final Pattern BUILD_SCRIPT_PATTERN = Pattern.compile("\"build\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEV_SCRIPT_PATTERN = Pattern.compile("\"dev\"\\s*:\\s*\"([^\"]+)\"");

    public static class CommandResult {
        private final String buildCommand;
        private final String devCommand;
        private final boolean executed = false;

        public CommandResult(String buildCommand, String devCommand) {
            this.buildCommand = buildCommand;
            this.devCommand = devCommand;
        }

        public String getBuildCommand() {
            return buildCommand;
        }

        public String getDevCommand() {
            return devCommand;
        }

        public boolean isExecuted() {
            return executed;
        }
    }

    public CommandResult detect(ProjectSourceReader source, String packageManager) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isEmpty()) {
            return new CommandResult("Not detected", "Not detected");
        }

        String pkgJson = pkgJsonOpt.get();
        String pm = (packageManager != null && !packageManager.equals("UNKNOWN")) ? packageManager.toLowerCase() : "npm";
        String runPrefix = pm.equals("npm") ? "npm run " : pm + " ";

        String buildCmd = "Not detected";
        Matcher buildMatcher = BUILD_SCRIPT_PATTERN.matcher(pkgJson);
        if (buildMatcher.find()) {
            buildCmd = runPrefix + "build";
        }

        String devCmd = "Not detected";
        Matcher devMatcher = DEV_SCRIPT_PATTERN.matcher(pkgJson);
        if (devMatcher.find()) {
            devCmd = runPrefix + "dev";
        }

        return new CommandResult(buildCmd, devCmd);
    }
}
