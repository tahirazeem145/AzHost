package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnknownFrameworkDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        return new FrameworkDetectionResult(
                true,
                ProjectFramework.UNKNOWN,
                "Unknown",
                DetectionConfidence.LOW,
                List.of("Unable to match supported framework indicators in project files")
        );
    }
}
