package io.github.kxng0109.taskflow.project;

import io.github.kxng0109.taskflow.project.dto.AddMemberRequest;
import io.github.kxng0109.taskflow.project.dto.ProjectRequest;
import io.github.kxng0109.taskflow.project.dto.ProjectResponse;
import io.github.kxng0109.taskflow.project.dto.UserSummaryResponse;
import io.github.kxng0109.taskflow.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest project,
            @AuthenticationPrincipal User currentUser
    ) {
        Project newProject = projectService.createProject(project, currentUser);
        return new ResponseEntity<>(convertProjectToResponse(newProject), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getProjectsForUser(
            @AuthenticationPrincipal User currentUser
    ){
        List<Project> projectsForUser = projectService.getProjectsForUser(currentUser);
        return ResponseEntity.ok(projectsForUser);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ){
        Project project = projectService.getProjectById(projectId, currentUser);
        return ResponseEntity.ok(convertProjectToResponse(project));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectRequest updateRequest,
            @AuthenticationPrincipal User currentUser
    ){
        Project updatedProject = projectService.updateProject(
                projectId, updateRequest, currentUser
        );
        return ResponseEntity.ok(convertProjectToResponse(updatedProject));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ){
        projectService.deleteProject(projectId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectResponse> addMemberToProject(
            @PathVariable Long projectId,
            @RequestBody AddMemberRequest member,
            @AuthenticationPrincipal User currentUser
    ){
        Project updatedProject = projectService.addMemberToProject(projectId, member, currentUser);
        return new ResponseEntity<>(convertProjectToResponse(updatedProject), HttpStatus.CREATED);
    }

    private ProjectResponse convertProjectToResponse(Project project){
        List<UserSummaryResponse> members = project.getMembers().stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getName()))
                .toList();

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                members
        );
    }
}
