package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.*;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.ProjectStatus;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.security.WebSecurityConfig;
import com.azhost.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    private ProjectResponseDto sampleDto;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleDto = new ProjectResponseDto(
                sampleId,
                "TripNest",
                "tripnest",
                "Travel App",
                ProjectFramework.REACT,
                ProjectSourceType.GITHUB,
                "https://github.com/example/tripnest",
                "main",
                ProjectStatus.ACTIVE,
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );
    }

    @Test
    void createProject_ShouldReturn201Created() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("TripNest", "Travel App", ProjectFramework.REACT, ProjectSourceType.GITHUB, "https://github.com/example/tripnest", "main");

        given(projectService.createProject(any(CreateProjectRequest.class), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(sampleDto);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TripNest"))
                .andExpect(jsonPath("$.slug").value("tripnest"));
    }

    @Test
    void getProjects_ShouldReturn200OK() throws Exception {
        ProjectListResponseDto responseDto = new ProjectListResponseDto(List.of(sampleDto), 1L);

        given(projectService.getProjects(eq(DevUserInitializer.DEV_USER_EMAIL), any()))
                .willReturn(responseDto);

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.projects[0].name").value("TripNest"));
    }

    @Test
    void getProjectById_WhenFound_ShouldReturn200OK() throws Exception {
        given(projectService.getProjectById(eq(sampleId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(sampleDto);

        mockMvc.perform(get("/api/projects/" + sampleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.name").value("TripNest"));
    }

    @Test
    void getProjectById_WhenNotFound_ShouldReturn404() throws Exception {
        UUID missingId = UUID.randomUUID();
        given(projectService.getProjectById(eq(missingId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willThrow(new ProjectNotFoundException("Project not found"));

        mockMvc.perform(get("/api/projects/" + missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteProject_ShouldReturn204NoContent() throws Exception {
        doNothing().when(projectService).deleteProject(eq(sampleId), eq(DevUserInitializer.DEV_USER_EMAIL));

        mockMvc.perform(delete("/api/projects/" + sampleId))
                .andExpect(status().isNoContent());
    }
}
