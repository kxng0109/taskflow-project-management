package io.github.kxng0109.taskflow.project;

import io.github.kxng0109.taskflow.project.dto.AddMemberRequest;
import io.github.kxng0109.taskflow.project.dto.ProjectRequest;
import io.github.kxng0109.taskflow.user.User;
import io.github.kxng0109.taskflow.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User testUserAMember;
    private User testUserNotAMember;
    private Set<User> testMembers;
    private final Long projectId = 100L;

    @BeforeEach
    public void setup() {
        testMembers = new HashSet<>();

        testUserAMember = User.builder()
                .id(1L)
                .name("a member")
                .email("amember@email.com")
                .build();

        testUserNotAMember = User.builder()
                .id(2L)
                .name("not a member")
                .email("notamember@email.com")
                .build();

        testMembers.add(testUserAMember);
    }

    @Test
    void createProject_should_returnProject_whenUserIsAuthenticated(){
        ProjectRequest projectRequest = new ProjectRequest("projectName", "projectDescription");

        when(projectRepository.save(any(Project.class)))
                .thenAnswer(i -> i.getArgument(0));

        Project result = projectService.createProject(projectRequest, testUserAMember);

        assertNotNull(result);
        assertEquals(result.getName(), projectRequest.name());
        assertEquals(result.getDescription(), projectRequest.description());
        assertTrue(result.getMembers().contains(testUserAMember));

        verify(projectRepository).save(any(Project.class));
    }


    @Test
    void getProjectById_should_returnProject_whenUserIsAMemberOfTheProject(){
        Project existingProject = Project.builder().id(projectId).members(testMembers).build();

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));

        Project result = projectService.getProjectById(projectId, testUserAMember);

        assertNotNull(result);
        assertEquals(existingProject.getId(), result.getId());
        assertTrue(result.getMembers().contains(testUserAMember));

        verify(projectRepository).findById(projectId);
    }

    @Test
    void getProjectById_should_throwAccessDeniedException_whenUserIsNotAMemberOfTheProject(){
        Project existingProject = Project.builder().id(projectId).members(testMembers).build();
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> projectService.getProjectById(projectId, testUserNotAMember)
        );

        assertEquals("You are not a member of this project", thrownException.getMessage());

        verify(projectRepository).findById(projectId);
    }

    @Test
    void getProjectById_should_throwEntityNotFoundException_whenProjectIsNotFound(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.getProjectById(projectId, testUserAMember)
        );

        assertEquals("Project with id " + projectId + " not found", thrownException.getMessage());
        verify(projectRepository).findById(projectId);
    }

    @Test
    void getProjectsForUser_should_returnProjects_whenUserIsAMemberOfProject(){
        Project existingProject = Project.builder().id(projectId).members(testMembers).build();
        List<Project> projects = new ArrayList<>();
        projects.add(existingProject);

        when(projectRepository.findByMembersContaining(testUserAMember))
                .thenReturn(projects);

        List<Project> result = projectService.getProjectsForUser(testUserAMember);

        assertNotNull(result);
        assertEquals(projects.size(), result.size());
        assertTrue(result.containsAll(projects));
        assertTrue(result.getFirst().getMembers().contains(testUserAMember));

        verify(projectRepository).findByMembersContaining(testUserAMember);
    }

    @Test
    void getProjectsForUser_should_returnNoProject_whenUserIsNotAMemberOfAnyProject(){
        when(projectRepository.findByMembersContaining(testUserNotAMember))
                .thenReturn(new ArrayList<>());

        List<Project> result = projectService.getProjectsForUser(testUserNotAMember);

        assertTrue(result.isEmpty());

        verify(projectRepository).findByMembersContaining(testUserNotAMember);
    }


    @Test
    void updateProject_should_returnUpdatedProject_whenUserIsAMemberOfTheProject(){
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .name("oldName")
                .description("oldDescription")
                .build();
        
        ProjectRequest updatedProjectRequest = new ProjectRequest("newName", "newDescription");

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));
        when(projectRepository.save(existingProject))
                .thenAnswer(i -> i.getArgument(0));

        Project result = projectService.updateProject(
                existingProject.getId(), 
                updatedProjectRequest, 
                testUserAMember
        );

        assertNotNull(result);
        assertEquals(result.getName(), updatedProjectRequest.name());
        assertEquals(result.getDescription(), updatedProjectRequest.description());
        assertTrue(result.getMembers().contains(testUserAMember));

        verify(projectRepository).findById(projectId);
        verify(projectRepository).save(existingProject);
    }

    @Test
    void updateProject_should_throwAccessDeniedException_whenUserIsNotAMemberOfTheProject(){
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .build();

        ProjectRequest updatedProjectRequest = new ProjectRequest("newName", "newDescription");

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> projectService.updateProject(
                        existingProject.getId(),
                        updatedProjectRequest,
                        testUserNotAMember
                )
        );

        assertEquals("You are not a member of this project", thrownException.getMessage());

        verify(projectRepository).findById(projectId);
    }

    @Test
    void updateProject_should_throwNotFoundException_whenProjectIsNotFound(){
        ProjectRequest updatedProjectRequest = new ProjectRequest("newName", "newDescription");

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.empty());
        EntityNotFoundException thrownError = assertThrows(EntityNotFoundException.class, () -> projectService.updateProject(projectId, updatedProjectRequest, testUserAMember));

        assertEquals("Project with id " + projectId + " not found", thrownError.getMessage());
        verify(projectRepository).findById(projectId);
    }


    @Test
    void deleteProject_should_deleteProject_whenUserIsAMemberOfProject(){
        Project existingProject = Project.builder().id(projectId).members(testMembers).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        projectService.deleteProject(projectId, testUserAMember);

        verify(projectRepository).findById(projectId);
        verify(projectRepository).delete(existingProject);
    }

    @Test
    void deleteProject_should_throwAccessDeniedException_whenUserIsNotAMemberOfProject(){
        Project existingProject = Project.builder().id(projectId).members(testMembers).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        AccessDeniedException thrownException = assertThrows(AccessDeniedException.class, () -> projectService.deleteProject(projectId, testUserNotAMember));

        assertEquals("You are not a member of this project", thrownException.getMessage());

        verify(projectRepository).findById(projectId);
    }

    @Test
    void deleteProject_should_throwNotFoundException_whenProjectIsNotFound(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        EntityNotFoundException thrownError = assertThrows(EntityNotFoundException.class, () -> projectService.deleteProject(projectId, testUserAMember));

        assertEquals("Project with id " + projectId + " not found", thrownError.getMessage());

        verify(projectRepository).findById(projectId);
    }


    @Test
    void addMemberToProject_should_returnProjectAndAddMember_whenUserIsAMemberOfProject(){
        testMembers.add(testUserAMember);
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .build();

        AddMemberRequest newMemberRequest = new AddMemberRequest(testUserNotAMember.getEmail());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(userRepository.findByEmail(newMemberRequest.email()))
                .thenReturn(Optional.ofNullable(testUserNotAMember));
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project result = projectService.addMemberToProject(projectId, newMemberRequest, testUserAMember);

        assertNotNull(result);
        assertTrue(result.getMembers().contains(testUserNotAMember));
        assertEquals(result.getMembers().size(), testMembers.size());

        verify(projectRepository).findById(projectId);
        verify(userRepository).findByEmail(newMemberRequest.email());
        verify(projectRepository).save(existingProject);
    }

    @Test
    void addMemberToProject_should_throwAccessDeniedException_whenUserIsNotAMemberOfProject(){
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .build();
        AddMemberRequest newMemberRequest = new AddMemberRequest(testUserNotAMember.getEmail());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> projectService.addMemberToProject(projectId, newMemberRequest, testUserNotAMember)
        );

        assertEquals("You are not a member of this project", thrownException.getMessage());

        verify(projectRepository).findById(projectId);

    }

    @Test
    void addMemberToProject_should_throwEntityNotFoundException_whenAddedUserDoesNotExist(){
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .build();
        AddMemberRequest newMemberRequest = new AddMemberRequest(testUserNotAMember.getEmail());

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));
        when(userRepository.findByEmail(newMemberRequest.email()))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.addMemberToProject(projectId, newMemberRequest, testUserAMember)
        );

        assertEquals("User not found with email: " + newMemberRequest.email(), thrownException.getMessage());

        verify(projectRepository).findById(projectId);
        verify(userRepository).findByEmail(newMemberRequest.email());
    }

    @Test
    void addMemberToProject_should_throwIllegalStateException_whenUserIsAlreadyAMember(){
        Project existingProject = Project.builder()
                .id(projectId)
                .members(testMembers)
                .build();
        existingProject.getMembers().add(testUserNotAMember);
        AddMemberRequest newMemberRequest = new AddMemberRequest(testUserNotAMember.getEmail());

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(existingProject));
        when(userRepository.findByEmail(newMemberRequest.email()))
                .thenReturn(Optional.of(testUserNotAMember));

        IllegalStateException thrownException = assertThrows(
                IllegalStateException.class,
                () -> projectService.addMemberToProject(projectId, newMemberRequest, testUserNotAMember)
        );

        assertEquals("User is already member of this project", thrownException.getMessage());
        assertTrue(existingProject.getMembers().contains(testUserNotAMember));
        assertEquals(existingProject.getMembers().size(), testMembers.size());

        verify(projectRepository).findById(projectId);
        verify(userRepository).findByEmail(newMemberRequest.email());
    }

    @Test
    void addMemberToProject_should_throwEntityNotFoundException_whenProjectDoesNotExist(){
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        AddMemberRequest newMemberRequest = new AddMemberRequest(testUserNotAMember.getEmail());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> projectService.addMemberToProject(projectId, newMemberRequest, testUserNotAMember)
        );

        assertEquals("Project with id " + projectId + " not found", thrownException.getMessage());
        verify(projectRepository).findById(projectId);
    }
}
