package com.azhost.analysis.detector;

import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

@Component
public class OutputDirectoryDetector {

    public String detectOutputDirectory(ProjectFramework framework, String buildTool) {
        if (framework == null) {
            return ".";
        }

        switch (framework) {
            case REACT:
                if ("Create React App".equalsIgnoreCase(buildTool)) {
                    return "build";
                }
                return "dist";
            case VITE:
            case VUE:
                return "dist";
            case NEXT_JS:
                return ".next";
            case ANGULAR:
                return "dist";
            case STATIC:
                return ".";
            default:
                return ".";
        }
    }
}
