package io.github.kxng0109.taskflow.task;

import io.github.kxng0109.taskflow.project.Project;
import io.github.kxng0109.taskflow.project.ProjectRepository;
import io.github.kxng0109.taskflow.task.dto.TaskRequest;
import io.github.kxng0109.taskflow.user.User;
import io.github.kxng0109.taskflow.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Task createTaskInProject(Long projectId, TaskRequest taskRequest, User currentUser) {
        Project project = getProjectAndVerifyMembership(projectId, currentUser);

        User assignee = null;
        if(taskRequest.assigneeId() != null){
            assignee = userRepository.findById(taskRequest.assigneeId())
                    .orElseThrow(()->new IllegalArgumentException("User with id " + taskRequest.assigneeId() + " not found"));

            if(!project.getMembers().contains(assignee)) {
                throw new IllegalArgumentException("Cannot assign task to a user who is not a member of this project");
            }
        }

        Task taskToAdd = Task.builder()
                .title(taskRequest.title())
                .description(taskRequest.description())
                .status(TaskStatus.valueOf(taskRequest.status()))
                .project(project)
                .assignee(assignee)
                .build();

        return taskRepository.save(taskToAdd);
    }

    public List<Task> getTasksForProject(Long projectId, User curentUser) {
        Project project = getProjectAndVerifyMembership(projectId, curentUser);
        return project.getTasks();
    }

    public Task getTaskById(Long projectId, Long taskId, User currentUser) {
        return getTaskAndVerifyMembership(projectId, taskId, currentUser);
    }

    @Transactional
    public Task updateTaskInProject(Long projectId, Long taskId, TaskRequest taskUpdate, User currentUser) {
        Task taskToUpdate = getTaskAndVerifyMembership(projectId, taskId, currentUser);

        taskToUpdate.setTitle(taskUpdate.title());
        taskToUpdate.setDescription(taskUpdate.description());
        taskToUpdate.setStatus(TaskStatus.valueOf(taskUpdate.status()));

        if(taskUpdate.assigneeId() == null){
            taskToUpdate.setAssignee(null);
            return taskRepository.save(taskToUpdate);
        }

        User newAssignee = userRepository.findById(taskUpdate.assigneeId())
                .orElseThrow(
                        ()-> new EntityNotFoundException("User with id " + taskUpdate.assigneeId() + " not found")
                );

        if(!taskToUpdate.getProject().getMembers().contains(newAssignee)) {
            throw new AccessDeniedException("Cannot assign task to a user who is not a member of this project");
        }

        taskToUpdate.setAssignee(newAssignee);
        return taskRepository.save(taskToUpdate);
    }

    @Transactional
    public void deleteTaskInProject(Long projectId, Long taskId, User currentUser) {
        Task task = getTaskAndVerifyMembership(projectId, taskId, currentUser);
        taskRepository.delete(task);
    }

    private Project getProjectAndVerifyMembership(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + projectId + " not found"));

        if(!project.getMembers().contains(currentUser)){
            throw new AccessDeniedException("You are not a member of this task's project");
        }

        return project;
    }

    private Task getTaskAndVerifyMembership(Long projectId, Long taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task with id " + taskId + " not found"));

        //Find out if the task actually belongs to the project
        if(!task.getProject().getId().equals(projectId)){
            throw new AccessDeniedException("This task does not belong to this project");
        }

        //Then the usual to find out if the current user belongs to the project
        if(!task.getProject().getMembers().contains(currentUser)){
            throw new AccessDeniedException("You are not a member of this task's project");
        }

        return task;
    }
}
