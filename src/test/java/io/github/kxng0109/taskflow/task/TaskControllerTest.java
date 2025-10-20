package io.github.kxng0109.taskflow.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.kxng0109.taskflow.project.Project;
import io.github.kxng0109.taskflow.project.ProjectRepository;
import io.github.kxng0109.taskflow.security.dto.LoginRequest;
import io.github.kxng0109.taskflow.task.dto.TaskRequest;
import io.github.kxng0109.taskflow.user.User;
import io.github.kxng0109.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class TaskControllerTest {
    private final String basePath = "/api/projects/{projectId}";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    private User testUser;
    private User testUser2;

    @BeforeEach
    public void setup() {
        testUser = User.builder()
                       .name("testName")
                       .password(passwordEncoder.encode("testPassword"))
                       .email("testEmail@email.com")
                       .build();

        testUser2 = User.builder()
                        .name("testName2")
                        .password(passwordEncoder.encode("testPassword2"))
                        .email("testEmail2@email.com")
                        .build();

        userRepository.save(testUser);
        userRepository.save(testUser2);
    }

    private String loginAndGetToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), "testPassword");

        String result = mockMvc.perform(post("/api/auth/login")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                               .andExpect(status().isOk())
                               .andReturn().getResponse().getContentAsString();

        return JsonPath.parse(result).read("$.accessToken");
    }

    @Test
    void createTaskInProject_should_return200OkAndTask_whenUserIsAuthenticatedAndMember() throws Exception {
        Project project = setupProjectWithMember();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                null
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.title").value("new title"))
               .andExpect(jsonPath("$.projectId").value(project.getId()));
    }

    @Test
    void createTaskInProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Project project = setupProjectWithMember();

        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                null
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void createTaskInProject_should_throw400MethodArgumentNotValidException_whenRequestIsMalformed() throws Exception {
        Project project = setupProjectWithMember();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "a title",
                "new title",
                "",
                null
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createTaskInProject_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                null
        );

        mockMvc.perform(post(basePath + "/tasks", 12345L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    void createTaskInProject_should_throw403AccessDeniedException_whenUserIsNotAMember() throws Exception {
        Project project = setupOtherProjectWithOtherMember();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                null
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }

    @Test
    void createTaskInProject_should_throw_404EntityNotFoundException_whenAssigneeIsNotFound() throws Exception {
        Project project = setupProjectWithMember();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                12345L
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    void createTaskInProject_should_throw403AccessDeniedException_whenAssigneeIsNotAMember() throws Exception {
        Project project = setupOtherProjectWithOtherMember();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                testUser2.getId()
        );

        mockMvc.perform(post(basePath + "/tasks", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }


    @Test
    void getTasksForProject_should_return200OkAndTasks_whenUserIsAuthenticatedAndMember() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks", task.getProject().getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.[0].id").value(task.getId()))
               .andExpect(jsonPath("$.[0].description").value(task.getDescription()));
    }

    @Test
    void getTasksForProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Task task = setupTaskInProject();

        mockMvc.perform(get(basePath + "/tasks", task.getProject().getId()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void getTasksForProject_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks", 12345L)
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    void getTasksForProject_should_throw403AccessDeniedException_whenUserIsAuthenticatedAndNotAMember() throws Exception {
        Project project = setupOtherProjectWithOtherMember();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks", project.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }


    @Test
    void getTaskById_should_return200OkAndTask_whenUserIsAuthenticatedAndMember() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(task.getId()))
               .andExpect(jsonPath("$.description").value(task.getDescription()))
               .andExpect(jsonPath("$.projectId").value(task.getProject().getId()));
    }

    @Test
    void getTaskById_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Task task = setupTaskInProject();

        mockMvc.perform(get(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void getTaskById_should_throw404EntityNotFoundException_whenTaskIsNotFound() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks/{taskId}", task.getProject().getId(), 12345L)
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    void getTaskById_should_throw403AccessDeniedException_whenTaskDoesNotBelongInProject() throws Exception {
        Project project = setupProjectWithMember();
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks/{taskId}", project.getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }

    @Test
    void getTaskById_should_throw403AccessDeniedException_whenUserIsAuthenticatedAndNotAMember() throws Exception {
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();

        mockMvc.perform(get(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }


    @Test
    void updateTaskInProject_should_return200OkAndUpdatedTask_whenUserIsAuthenticatedAndMember() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(task.getId()))
               .andExpect(jsonPath("$.description").value(task.getDescription()))
               .andExpect(jsonPath("$.projectId").value(task.getProject().getId()));
    }

    @Test
    void updateTaskInProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Task task = setupTaskInProject();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.TO_DO.name(),
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTaskInProject_should_throw400MethodArgumentNotValidException_whenRequestBodyIsMalformed() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "",
                "new description",
                "to do",
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateTaskInProject_should_throw404EntityNotFoundException_whenTaskDoesNotExist() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), 12345L)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isNotFound());
    }

    @Test
    void updateTaskInProject_should_throw403AccessDeniedException_whenTaskDoesNotBelongInProject() throws Exception {
        Project project = setupProjectWithMember();
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", project.getId(), task.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateTaskInProject_should_throw403AccessDeniedException_whenUserIsAuthenticatedAndNotAMember() throws Exception {
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                null
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateTaskInProject_should_throw404EntityNotFoundException_whenAssigneeIsNotFound() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                12345L
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .content(objectMapper.writeValueAsString(taskRequest))
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }

    @Test
    void updateTaskInProject_should_throw403AccessDeniedException_whenAssigneeIsNotAMember() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();
        TaskRequest taskRequest = new TaskRequest(
                "new title",
                "new description",
                TaskStatus.IN_PROGRESS.name(),
                testUser2.getId()
        );

        mockMvc.perform(put(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token)
                                .content(
                                        objectMapper.writeValueAsString(taskRequest)))
               .andExpect(status().isForbidden());
    }


    @Test
    void deleteTaskInProject_should_return204NoContent_whenUserIsAuthenticatedAndTaskExists() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();

        mockMvc.perform(delete(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaskInProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Task task = setupTaskInProject();

        mockMvc.perform(delete(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTaskInProject_should_throw404EntityNotFoundException_whenTaskIsNotFound() throws Exception {
        Task task = setupTaskInProject();
        String token = loginAndGetToken();

        mockMvc.perform(delete(basePath + "/tasks/{taskId}", task.getProject().getId(), 12345L)
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isNotFound());
    }

    @Test
    void deleteTaskInProject_should_throw403AccessDeniedException_whenTaskDoesNotBelongInProject() throws Exception {
        Project project = setupProjectWithMember();
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();

        mockMvc.perform(delete(basePath + "/tasks/{taskId}", project.getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }

    @Test
    void deleteTaskInProject_should_throw403AccessDeniedException_whenUserIsAuthenticatedAndNotAMember() throws Exception {
        Task task = setupTaskInOtherProject();
        String token = loginAndGetToken();

        mockMvc.perform(delete(basePath + "/tasks/{taskId}", task.getProject().getId(), task.getId())
                                .header("Authorization", "Bearer " + token))
               .andExpect(status().isForbidden());
    }


    private Project setupProjectWithMember() {
        Set<User> members = new HashSet<>();
        members.add(testUser);
        Project project = Project.builder()
                                 .name("testProject")
                                 .description("testDescription")
                                 .members(members)
                                 .build();
        return projectRepository.save(project);
    }

    private Project setupOtherProjectWithOtherMember() {
        Set<User> members = new HashSet<>();
        members.add(testUser2);
        Project project = Project.builder()
                                 .name("testProject2")
                                 .description("testDescription2")
                                 .members(members)
                                 .build();
        return projectRepository.save(project);
    }

    private Task setupTaskInProject() {
        Project project = setupProjectWithMember();

        Task task = Task.builder()
                        .title("testTask")
                        .description("testDescription")
                        .project(project)
                        .status(TaskStatus.IN_PROGRESS)
                        .build();
        project.addTask(task);
        return task;
    }

    private Task setupTaskInOtherProject() {
        Project project = setupOtherProjectWithOtherMember();

        Task task = Task.builder()
                        .title("testTask")
                        .description("testDescription")
                        .project(project)
                        .status(TaskStatus.IN_PROGRESS)
                        .build();
        project.addTask(task);
        return task;
    }
}
