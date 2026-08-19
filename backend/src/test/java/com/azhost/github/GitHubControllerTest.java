package com.azhost.github;

import com.azhost.config.DevUserInitializer;
import com.azhost.entity.User;
import com.azhost.github.controller.GitHubController;
import com.azhost.github.dto.GitHubBranchDto;
import com.azhost.github.dto.GitHubConnectionResponseDto;
import com.azhost.github.dto.GitHubRepositoryDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.security.GitHubSecurityPolicy;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GitHubController.class)
@AutoConfigureMockMvc(addFilters = false)
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubOAuthService oauthService;

    @MockBean
    private GitHubRepositoryService repositoryService;

    @MockBean
    private GitHubSecurityPolicy securityPolicy;

    @MockBean
    private GitHubTokenEncryptor tokenEncryptor;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(DevUserInitializer.DEV_USER_EMAIL, "hash", "Dev");
        testUser.setId(UUID.randomUUID());
        when(userRepository.findByEmail(DevUserInitializer.DEV_USER_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @Test
    void shouldReturnOAuthConnectUrl() throws Exception {
        when(oauthService.generateConnectUrl(DevUserInitializer.DEV_USER_EMAIL))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=123&state=abc");

        mockMvc.perform(get("/api/github/connect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/login/oauth/authorize?client_id=123&state=abc"));
    }

    @Test
    void shouldReturnGitHubConnectionStatus() throws Exception {
        GitHubConnectionEntity entity = new GitHubConnectionEntity(testUser, 12345L, "octocat", "https://avatar.url", "enc", "scope");
        GitHubConnectionResponseDto dto = new GitHubConnectionResponseDto(entity);
        when(oauthService.getConnection(DevUserInitializer.DEV_USER_EMAIL)).thenReturn(dto);

        mockMvc.perform(get("/api/github/connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.githubUsername").value("octocat"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url"));
    }

    @Test
    void shouldDisconnectGitHubConnection() throws Exception {
        doNothing().when(oauthService).disconnect(DevUserInitializer.DEV_USER_EMAIL);

        mockMvc.perform(delete("/api/github/connection"))
                .andExpect(status().isNoContent());

        verify(oauthService).disconnect(DevUserInitializer.DEV_USER_EMAIL);
    }

    @Test
    void shouldReturnUserRepositories() throws Exception {
        GitHubRepositoryDto repo = new GitHubRepositoryDto(100L, "TripNest", "user/TripNest", true, "main", "https://github.com/user/TripNest", "2026-08-18");
        when(repositoryService.getUserRepositories(DevUserInitializer.DEV_USER_EMAIL)).thenReturn(List.of(repo));

        mockMvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].name").value("TripNest"))
                .andExpect(jsonPath("$[0].private").value(true));
    }

    @Test
    void shouldReturnRepositoryBranches() throws Exception {
        GitHubBranchDto branch = new GitHubBranchDto("main", true);
        when(repositoryService.getRepositoryBranches(DevUserInitializer.DEV_USER_EMAIL, 100L)).thenReturn(List.of(branch));

        mockMvc.perform(get("/api/github/repositories/100/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("main"))
                .andExpect(jsonPath("$[0].protected").value(true));
    }
}
