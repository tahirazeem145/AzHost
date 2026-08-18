package com.azhost.analysis;

import com.azhost.config.DevUserInitializer;
import com.azhost.controller.ProjectAnalysisController;
import com.azhost.dto.ProjectAnalysisResponseDto;
import com.azhost.entity.ProjectFramework;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.exception.ProjectSourceNotAvailableException;
import com.azhost.security.WebSecurityConfig;
import com.azhost.service.ProjectAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectAnalysisController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class ProjectAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectAnalysisService projectAnalysisService;

    private UUID sampleId;
    private ProjectAnalysisResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDto = new ProjectAnalysisResponseDto();
        sampleDto.setProjectId(sampleId);
        sampleDto.setFramework(ProjectFramework.REACT);
        sampleDto.setFrameworkConfidence(DetectionConfidence.HIGH);
        sampleDto.setBuildTool("Vite");
        sampleDto.setPackageManager("NPM");
        sampleDto.setPackageManagerConfidence(DetectionConfidence.HIGH);
        sampleDto.setLanguage("TYPESCRIPT");
        sampleDto.setBuildCommand("npm run build");
        sampleDto.setDevCommand("npm run dev");
        sampleDto.setOutputDirectory("dist");
        sampleDto.setNodeVersion(">=18");
        sampleDto.setConfidence(DetectionConfidence.HIGH);
        sampleDto.setExecuted(false);
        sampleDto.setEvidence(List.of("package.json contains react"));
        sampleDto.setWarnings(List.of());
        sampleDto.setDetectedFiles(List.of("package.json", "vite.config.ts"));
        sampleDto.setAnalyzedAt(ZonedDateTime.now());
    }

    @Test
    void analyzeProject_WhenSourceExists_ShouldReturn200OK() throws Exception {
        given(projectAnalysisService.analyzeProject(eq(sampleId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(sampleDto);

        mockMvc.perform(post("/api/projects/" + sampleId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.framework").value("REACT"))
                .andExpect(jsonPath("$.buildTool").value("Vite"))
                .andExpect(jsonPath("$.executed").value(false));
    }

    @Test
    void analyzeProject_WhenSourceNotAvailable_ShouldReturn409Conflict() throws Exception {
        given(projectAnalysisService.analyzeProject(eq(sampleId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willThrow(new ProjectSourceNotAvailableException("Project source is not available for analysis yet."));

        mockMvc.perform(post("/api/projects/" + sampleId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("PROJECT_SOURCE_NOT_AVAILABLE"));
    }

    @Test
    void getLatestAnalysis_WhenExists_ShouldReturn200OK() throws Exception {
        given(projectAnalysisService.getLatestAnalysis(eq(sampleId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(sampleDto);

        mockMvc.perform(get("/api/projects/" + sampleId + "/analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.framework").value("REACT"));
    }

    @Test
    void getLatestAnalysis_WhenNotFound_ShouldReturn404() throws Exception {
        UUID missingId = UUID.randomUUID();
        given(projectAnalysisService.getLatestAnalysis(eq(missingId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willThrow(new ProjectNotFoundException("Analysis result not found"));

        mockMvc.perform(get("/api/projects/" + missingId + "/analysis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
