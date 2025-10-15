package io.github.kxng0109.taskflow.project;

import io.github.kxng0109.taskflow.project.dto.AddMemberRequest;
import io.github.kxng0109.taskflow.project.dto.ProjectRequest;
import io.github.kxng0109.taskflow.user.User;
import io.github.kxng0109.taskflow.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(ProjectRequest projectRequest, User currentUser) {
       Project newProject = Project.builder()
                .name(projectRequest.name())
                .description(projectRequest.description())
                .build();

       Set<User> members = new HashSet<>();
       members.add(currentUser);
       newProject.setMembers(members);

       return projectRepository.save(newProject);
    }

    public Project getProjectById(Long projectId, User currentUser) {
        return getIfUserIsAMemberOfProject(projectId, currentUser);
    }

    public List<Project> getProjectsForUser(User currentUser) {
        return projectRepository.findByMembersContaining(currentUser);
    }

    public Project updateProject(Long projectId, ProjectRequest projectRequest, User currentUser) {
        Project project = getIfUserIsAMemberOfProject(projectId, currentUser);

        project.setName(projectRequest.name());
        if(projectRequest.description() != null && !projectRequest.description().isEmpty()){
            project.setDescription(projectRequest.description());
        }

        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId, User currentUser) {
        Project project =  getIfUserIsAMemberOfProject(projectId, currentUser);
        projectRepository.delete(project);
    }

    public Project addMemberToProject(Long projectId, AddMemberRequest addMemberRequest,  User currentUser) {
        Project project = getIfUserIsAMemberOfProject(projectId, currentUser);
        User userToAdd = userRepository.findByEmail(addMemberRequest.email())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + addMemberRequest.email()));

        if(project.getMembers().contains(userToAdd)) {
            throw new IllegalArgumentException("User is already member of this project");
        }

        //Get the existing set of members and add the new one
        project.getMembers().add(userToAdd);

        return projectRepository.save(project);
    }
    
    private Project getIfUserIsAMemberOfProject(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + projectId + " not found"));

        if(!project.getMembers().contains(currentUser)){
            throw new AccessDeniedException("You are not a member of this project");
        }
        
        return  project;
    }
}
