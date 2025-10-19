package io.github.kxng0109.taskflow.task;

import io.github.kxng0109.taskflow.project.Project;
import io.github.kxng0109.taskflow.project.ProjectRepository;
import io.github.kxng0109.taskflow.task.dto.TaskRequest;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private User testUserAMember;
    private User testUserNotAMember;
    private Project existingProject;
    private Task existingTask;
    private Task otherTask;
    private final Long fakeTaskId = 12345L;
    private final Long fakeUserId = 3L;
    private final Set<User> members = new HashSet<>();

    @BeforeEach
    public void setup() {
        testUserAMember = User.builder()
                .id(1L)
                .name("testUserAMember")
                .password("encodedPassword")
                .email("test@email.com")
                .build();
        members.add(testUserAMember);

        testUserNotAMember = User.builder()
                .id(2L)
                .name("testUserNotAMember")
                .password("encodedPassword2")
                .email("test2@email.com")
                .build();

        existingProject = Project.builder()
                .id(100L)
                .name("projectName")
                .description("projectDescription")
                .members(members)
                .build();

        existingTask = Task.builder()
                .id(1000L)
                .title("taskTitle")
                .description("taskDescription")
                .status(TaskStatus.IN_PROGRESS)
                .project(existingProject)
                .assignee(testUserAMember)
                .build();

        otherTask = Task.builder()
                .id(1000L)
                .title("otherTitle")
                .description("otherDescription")
                .status(TaskStatus.TO_DO)
                .project(Project.builder().id(200L).build())
                .assignee(testUserAMember)
                .build();

        existingProject.setTasks(List.of(existingTask));
    }

    @Test
    public void createTaskInProject_should_returnATask_whenUserIsAMember() {
        TaskRequest taskRequest = new TaskRequest("task 1", null,"TO_DO", testUserAMember.getId());

        when(projectRepository.findById(existingProject.getId()))
                .thenReturn(Optional.of(existingProject));
        when(userRepository.findById(testUserAMember.getId()))
                .thenReturn(Optional.of(testUserAMember));
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(i -> i.getArgument(0));

        Task result = taskService.createTaskInProject(existingProject.getId(), taskRequest, testUserAMember);

        assertNotNull(result);
        assertEquals(taskRequest.assigneeId(), result.getAssignee().getId());
        assertEquals(taskRequest.title(), result.getTitle());
        assertEquals(TaskStatus.TO_DO, result.getStatus());
        assertEquals(existingProject, result.getProject());

        verify(projectRepository).findById(existingProject.getId());
        verify(userRepository).findById(testUserAMember.getId());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    public void createTaskInProject_should_throwAccessDeniedException_whenUserIsNotAMember() {
        TaskRequest taskRequest = new TaskRequest("task 1", null,"TO_DO", testUserNotAMember.getId());

        when(projectRepository.findById(existingProject.getId()))
                .thenReturn(Optional.of(existingProject));
        when(userRepository.findById(testUserNotAMember.getId()))
                .thenReturn(Optional.of(testUserNotAMember));

        AccessDeniedException thrownError = assertThrows(
                AccessDeniedException.class,
                () -> taskService.createTaskInProject(existingProject.getId(), taskRequest, testUserAMember)
        );

        assertEquals("Cannot assign task to a user who is not a member of this project", thrownError.getMessage());

        verify(projectRepository).findById(existingProject.getId());
        verify(userRepository).findById(testUserNotAMember.getId());
    }

    @Test
    public void createTaskInProject_should_throwEntityNotFoundException_whenUserIsNotFound(){
        TaskRequest taskRequest = new TaskRequest("task 1", null,"TO_DO", 12345L);

        when(projectRepository.findById(existingProject.getId()))
                .thenReturn(Optional.of(existingProject));
        when(userRepository.findById(taskRequest.assigneeId()))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.createTaskInProject(existingProject.getId(), taskRequest, testUserAMember)
        );

        assertEquals("User with id " + taskRequest.assigneeId() + " not found", thrownException.getMessage());

        verify(projectRepository).findById(existingProject.getId());
        verify(userRepository).findById(taskRequest.assigneeId());
    }


    @Test
    public void getTasksForProject_should_returnTask_whenUserIsAMember() {
        when(projectRepository.findById(existingProject.getId()))
                .thenReturn(Optional.of(existingProject));

        List<Task> result = taskService.getTasksForProject(existingProject.getId(), testUserAMember);

        assertNotNull(result);
        assertTrue(result.contains(existingTask));
        assertEquals(testUserAMember, result.getFirst().getAssignee());
        assertEquals(existingProject, result.getFirst().getProject());

        verify(projectRepository).findById(existingProject.getId());
    }

    @Test
    public void getTasksForProject_should_throwAccessDeniedException_whenUserIsNotAMember() {
        when(projectRepository.findById(existingProject.getId()))
                .thenReturn(Optional.ofNullable(existingProject));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.getTasksForProject(existingProject.getId(), testUserNotAMember)
        );

        assertEquals("You are not a member of this task's project", thrownException.getMessage());

        verify(projectRepository).findById(existingProject.getId());
    }


    @Test
    public void getTaskById_should_returnTask_whenUserIsAMember() {
        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));

        Task result = taskService.getTaskById(existingProject.getId(), existingTask.getId(), testUserAMember);

        assertNotNull(result);
        assertEquals(existingTask, result);
        assertEquals(testUserAMember, result.getAssignee());
        assertEquals(existingProject, result.getProject());

        verify(taskRepository).findById(existingTask.getId());
    }

    @Test
    public void getTaskById_should_throwEntityNotFoundException_whenTaskIsNotFound() {
        when(taskRepository.findById(fakeTaskId))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.getTaskById(existingProject.getId(), fakeTaskId, testUserAMember)
        );

        assertEquals("Task with id " + fakeTaskId + " not found", thrownException.getMessage());

        verify(taskRepository).findById(fakeTaskId);
    }

    @Test
    public void getTaskById_should_thrownAccessDeniedException_whenTaskDoesNotBelongToProject() {
        when(taskRepository.findById(otherTask.getId()))
                .thenReturn(Optional.of(otherTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.getTaskById(existingProject.getId(), otherTask.getId(), testUserAMember)
        );

        assertEquals("This task does not belong to this project", thrownException.getMessage());

        verify(taskRepository).findById(otherTask.getId());
    }

    @Test
    public void getTaskById_should_throwAccessDeniedException_whenUserIsNotAMember() {
        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.ofNullable(existingTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.getTaskById(existingProject.getId(), existingTask.getId(), testUserNotAMember)
        );

        assertEquals("You are not a member of this task's project", thrownException.getMessage());

        verify(taskRepository).findById(existingTask.getId());
    }


    @Test
    public void updateTaskInProject_should_removeAssigneeAndReturnTask_whenUserIsAMember() {
        TaskRequest taskRequest = new TaskRequest(
                "task title",
                "task description",
                TaskStatus.DONE.name(),
                null);

        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(i -> i.getArgument(0));

        Task result = taskService.updateTaskInProject(
                existingProject.getId(),
                existingTask.getId(),
                taskRequest,
                testUserAMember
        );

        assertNotNull(result);
        assertEquals(result.getTitle(), taskRequest.title());
        assertEquals(result.getDescription(), taskRequest.description());
        assertEquals(taskRequest.status(), result.getStatus().name());
        assertEquals(existingTask, result);
        assertNull(result.getAssignee());
        assertEquals(existingProject, result.getProject());

        verify(taskRepository).findById(existingTask.getId());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    public void updateTaskInProject_should_throwEntityNotFoundException_whenTaskDoesNotExist() {
        TaskRequest taskRequest = new TaskRequest(
                "task title",
                "task description",
                TaskStatus.DONE.name(),
                null
        );

        when(taskRepository.findById(fakeTaskId))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateTaskInProject(
                        existingProject.getId(),
                        fakeTaskId,
                        taskRequest,
                        testUserAMember
                )
        );

        assertEquals("Task with id " + fakeTaskId + " not found", thrownException.getMessage());
        verify(taskRepository).findById(fakeTaskId);
    }

    @Test
    public void updateTaskInProject_should_throwAccessDeniedException_whenTaskDoesNotBelongToProject() {
        TaskRequest taskRequest = new TaskRequest(
                "task title",
                "task description",
                TaskStatus.DONE.name(),
                null
        );

        when(taskRepository.findById(otherTask.getId()))
                .thenReturn(Optional.of(otherTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateTaskInProject(
                        existingProject.getId(),
                        otherTask.getId(),
                        taskRequest,
                        testUserAMember
                )
        );

        assertEquals("This task does not belong to this project", thrownException.getMessage());

        verify(taskRepository).findById(otherTask.getId());
    }

    @Test
    public void updateTaskInProject_should_throwAccessDeniedException_whenUserIsNotAMember() {
        TaskRequest taskRequest = new TaskRequest(
                "task title",
                "task description",
                TaskStatus.DONE.name(),
                null
        );

        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateTaskInProject(
                        existingProject.getId(),
                        existingTask.getId(),
                        taskRequest,
                        testUserNotAMember
                )
        );

        assertEquals("You are not a member of this task's project", thrownException.getMessage());

        verify(taskRepository).findById(existingTask.getId());
    }

    @Test
    public void updateTaskInProject_should_throwEntityNotFoundException_whenAssigneeDoesNotExist() {
        TaskRequest taskRequest = new TaskRequest(
                "task title",
                "task description",
                TaskStatus.DONE.name(),
                fakeUserId
        );

        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));
        when(userRepository.findById(fakeUserId))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.updateTaskInProject(
                        existingProject.getId(),
                        existingTask.getId(),
                        taskRequest,
                        testUserAMember
                )
        );

        assertEquals("User with id " + fakeUserId + " not found", thrownException.getMessage());

        verify(taskRepository).findById(existingTask.getId());
        verify(userRepository).findById(fakeUserId);
    }

    @Test
    public void updateTaskInProject_should_throwAccessDeniedException_whenAssigneeDoesNotBelongToProject() {
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.DONE.name(),
                testUserNotAMember.getId()
        );

        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));
        when(userRepository.findById(testUserNotAMember.getId()))
                .thenReturn(Optional.of(testUserNotAMember));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.updateTaskInProject(
                        existingProject.getId(),
                        existingTask.getId(),
                        taskRequest,
                        testUserAMember
                )
        );

        assertEquals("Cannot assign task to a user who is not a member of this project", thrownException.getMessage());

        verify(taskRepository).findById(existingTask.getId());
        verify(userRepository).findById(testUserNotAMember.getId());
    }


    @Test
    public void deleteTaskInProject_should_deleteTask_whenUserIsAMember() {
        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));

        taskService.deleteTaskInProject(existingProject.getId(), existingTask.getId(), testUserAMember);

        verify(taskRepository).findById(existingTask.getId());
        verify(taskRepository).delete(existingTask);
    }

    @Test
     public void deleteTaskInProject_should_throwEntityNotFoundException_whenTaskDoesNotExist() {
        when(taskRepository.findById(fakeTaskId))
                .thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> taskService.deleteTaskInProject(existingProject.getId(), fakeTaskId, testUserAMember)
        );

        assertEquals("Task with id " + fakeTaskId + " not found", thrownException.getMessage());
        verify(taskRepository).findById(fakeTaskId);
    }

    @Test
    public void deleteTaskInProject_should_throwAccessDeniedException_whenTaskDoesNotBelongToProject() {
        when(taskRepository.findById(otherTask.getId()))
                .thenReturn(Optional.of(otherTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.deleteTaskInProject(existingProject.getId(), otherTask.getId(), testUserAMember)
        );

        assertEquals("This task does not belong to this project", thrownException.getMessage());

        verify(taskRepository).findById(otherTask.getId());
    }

    @Test
    public void deleteTaskInProject_should_throwAccessDeniedException_whenUserIsNotAMember() {
        when(taskRepository.findById(existingTask.getId()))
                .thenReturn(Optional.of(existingTask));

        AccessDeniedException thrownException = assertThrows(
                AccessDeniedException.class,
                () -> taskService.deleteTaskInProject(existingProject.getId(), existingTask.getId(), testUserNotAMember)
        );

        assertEquals("You are not a member of this task's project", thrownException.getMessage());

        verify(taskRepository).findById(existingTask.getId());
    }
}
