package com.azhost.service;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectMemberEntity;
import com.azhost.entity.ProjectRole;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.repository.ProjectMemberRepository;
import com.azhost.repository.ProjectRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectAuthorizationService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    public Project verifyAccess(UUID projectId, String userEmail, ProjectRole minimumRole) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        ProjectRole userRole = getRoleForUser(project, userEmail);
        if (userRole == null || !userRole.satisfies(minimumRole)) {
            throw new AccessDeniedException("Access denied: minimum role " + minimumRole + " required");
        }

        return project;
    }

    public ProjectRole getRoleForUser(Project project, String userEmail) {
        if (project.getUser() != null && userEmail.equalsIgnoreCase(project.getUser().getEmail())) {
            return ProjectRole.OWNER;
        }

        return memberRepository.findByProjectIdAndUserEmail(project.getId(), userEmail)
                .map(ProjectMemberEntity::getRole)
                .orElse(null);
    }
}
