package io.github.kxng0109.taskflow.task;

import io.github.kxng0109.taskflow.project.dto.UserSummaryResponse;
import io.github.kxng0109.taskflow.task.dto.TaskRequest;
import io.github.kxng0109.taskflow.task.dto.TaskResponse;
import io.github.kxng0109.taskflow.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class TaskController {
    private final TaskService taskService;

    public  TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponse> createTaskInProject(
            @Valid @RequestBody TaskRequest taskRequest,
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ){
        Task newTask = taskService.createTaskInProject(projectId, taskRequest, currentUser);
        return new ResponseEntity<>(convertTaskToTaskResponse(newTask), HttpStatus.CREATED);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksForProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ){
        List<Task> tasks = taskService.getTasksForProject(projectId, currentUser);
        List<TaskResponse> taskResponses = tasks.stream()
                .map(this::convertTaskToTaskResponse).toList();
        return ResponseEntity.ok(taskResponses);
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser
    ){
        Task task = taskService.getTaskById(projectId, taskId, currentUser);
        return ResponseEntity.ok(convertTaskToTaskResponse(task));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTaskInProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest taskRequest,
            @AuthenticationPrincipal User currentUser
    ){
        Task updatedTask = taskService.updateTaskInProject(projectId, taskId, taskRequest, currentUser);
        return ResponseEntity.ok(convertTaskToTaskResponse(updatedTask));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTaskInProject(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser
    ){
        taskService.deleteTaskInProject(projectId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private TaskResponse convertTaskToTaskResponse(Task task){
        UserSummaryResponse assigneeSummary = null;
        if(task.getAssignee() != null){
            assigneeSummary = new UserSummaryResponse(
                    task.getAssignee().getId(),
                    task.getAssignee().getName()
            );
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                assigneeSummary
        );
    }
}
