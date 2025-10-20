package io.github.kxng0109.taskflow.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.kxng0109.taskflow.project.dto.AddMemberRequest;
import io.github.kxng0109.taskflow.project.dto.ProjectRequest;
import io.github.kxng0109.taskflow.security.dto.LoginRequest;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String basePath = "/api/projects";
    private User testUser;

    @BeforeEach
    public void setup() {
        String testEmail = "testEmail@email.com";
        testUser = User.builder()
                       .name("testName")
                       .password(passwordEncoder.encode("testPassword"))
                       .email(testEmail)
                       .build();

        userRepository.save(testUser);
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
    void createProject_shouldReturn201Created_whenUserIsAuthenticated() throws Exception {
        ProjectRequest newProject = new ProjectRequest("newProject", "newDescription");

        mockMvc.perform(post(basePath)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newProject)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.name").value(newProject.name()))
               .andExpect(jsonPath("$.description").value(newProject.description()))
               .andExpect(jsonPath("$.members[*].name", hasItem(testUser.getName())));
    }

    @Test
    void createProject_should_throw400MethodArgumentNotValidException_whenRequiredFieldIsEmpty() throws Exception {
        ProjectRequest newProject = new ProjectRequest("", "newDescription");

        mockMvc.perform(post(basePath)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newProject)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void createProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        ProjectRequest newProject = new ProjectRequest("newProject", "newDescription");

        mockMvc.perform(post(basePath)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newProject)))
               .andExpect(status().isUnauthorized());
    }


    @Test
    void getProjectsForUser_should_return200Ok_whenUserIsAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        mockMvc.perform(get(basePath).header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].name").value(project.getName()))
               .andExpect(jsonPath("$[0].description").value(project.getDescription()))
               .andExpect(jsonPath("$[0].members[*].name", hasItem(testUser.getName())));
    }

    @Test
    void getProjectsForUser_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get(basePath)).andExpect(status().isUnauthorized());
    }


    @Test
    void getProjectById_should_return200Ok_whenUserIsAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        mockMvc.perform(get(basePath + "/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value(project.getName()))
               .andExpect(jsonPath("$.description").value(project.getDescription()))
               .andExpect(jsonPath("$.members[*].name", hasItem(testUser.getName())));
    }

    @Test
    void getProjectById_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Project project = setupProject();

        mockMvc.perform(get(basePath + "/{projectId}", project.getId()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void getProjectById_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        mockMvc.perform(get(basePath + "/{projectId}", 12453)
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isNotFound());
    }


    @Test
    void updateProject_should_return200Ok_whenUserIsAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        ProjectRequest updatedProject = new ProjectRequest("newProjectNewName", "newDescription");

        mockMvc.perform(put(basePath + "/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedProject)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value(updatedProject.name()))
               .andExpect(jsonPath("$.description").value(updatedProject.description()))
               .andExpect(jsonPath("$.members[*].name", hasItem(testUser.getName())));
    }

    @Test
    void updateProject_should_throw400MethodArgumentNotValidException_whenRequiredFieldIsEmpty() throws Exception {
        Project project = setupProjectAndAddTestUser();

        ProjectRequest updatedProject = new ProjectRequest("", "newDescription");

        mockMvc.perform(put(basePath + "/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedProject)))
               .andExpect(status().isBadRequest());
    }

    @Test
    void updateProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Project project = setupProject();

        ProjectRequest updatedProject = new ProjectRequest("newProjectNewName", "newDescription");

        mockMvc.perform(put(basePath + "/{projectId}", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedProject)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProject_should_throw403AccessDeniedException_whenUserIsNotAMemberOfTheProject() throws Exception {
        Project project = setupProject();
        ProjectRequest updatedProject = new ProjectRequest("newProjectNewName", "newDescription");

        mockMvc.perform(put(basePath + "/{projectId}", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .content(objectMapper.writeValueAsString(updatedProject)))
               .andExpect(status().isForbidden());
    }

    @Test
    void updateProject_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        ProjectRequest updatedProject = new ProjectRequest("newProjectNewName", "newDescription");

        mockMvc.perform(put(basePath + "/{projectId}", 12453)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .content(objectMapper.writeValueAsString(updatedProject)))
               .andExpect(status().isNotFound());
    }


    @Test
    void deleteProject_should_return204NoContent_whenUserIsAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        mockMvc.perform(delete(basePath + "/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Project project = setupProject();

        mockMvc.perform(delete(basePath + "/{projectId}", project.getId()))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProject_should_throw403AccessDeniedException_whenUserIsNotAMemberOfProject() throws Exception {
        Project project = setupProject();

        mockMvc.perform(delete(basePath + "/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        mockMvc.perform(delete(basePath + "/{projectId}", 12453)
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isNotFound());
    }


    @Test
    void addMemberToProject_should_return200Ok_whenUserIsAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        User newUserToAdd = User.builder()
                                .name("otherUser")
                                .password(passwordEncoder.encode("otherPassword"))
                                .email("otherEmail@email.com")
                                .build();
        userRepository.save(newUserToAdd);

        AddMemberRequest addMemberRequest = new AddMemberRequest(newUserToAdd.getEmail());

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addMemberRequest)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.members[*].name", hasItem(newUserToAdd.getName())))
               .andExpect(jsonPath("$.members", hasSize(2)))
               .andExpect(jsonPath("$.description").value(project.getDescription()));
    }

    @Test
    void addMemberToProject_should_throw400MethodArgumentNotValidException_whenRequiredFieldIsMissing() throws Exception {
        Project project = setupProjectAndAddTestUser();

        AddMemberRequest addMemberRequest = new AddMemberRequest("");

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addMemberRequest))
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isBadRequest());
    }

    @Test
    void addMemberToProject_should_throw401Unauthorized_whenUserIsNotAuthenticated() throws Exception {
        Project project = setupProjectAndAddTestUser();

        User newUserToAdd = User.builder()
                                .name("otherUser")
                                .password(passwordEncoder.encode("otherPassword"))
                                .email("otherEmail@email.com")
                                .build();
        userRepository.save(newUserToAdd);

        AddMemberRequest addMemberRequest = new AddMemberRequest(newUserToAdd.getEmail());

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addMemberRequest)))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void addMemberToProject_should_throw403AccessDeniedException_whenUserIsNotAMemberOfProject() throws Exception {
        Project project = setupProject();

        User newUserToAdd = User.builder()
                                .name("otherUser")
                                .password(passwordEncoder.encode("otherPassword"))
                                .email("otherEmail@email.com")
                                .build();
        userRepository.save(newUserToAdd);

        AddMemberRequest addMemberRequest = new AddMemberRequest(newUserToAdd.getEmail());

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addMemberRequest))
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isForbidden());
    }

    @Test
    void addMemberToProject_should_throw404EntityNotFoundException_whenProjectIsNotFound() throws Exception {
        User newUserToAdd = User.builder()
                                .name("otherUser")
                                .password(passwordEncoder.encode("otherPassword"))
                                .email("otherEmail@email.com")
                                .build();
        userRepository.save(newUserToAdd);

        AddMemberRequest addMemberRequest = new AddMemberRequest(newUserToAdd.getEmail());

        mockMvc.perform(post(basePath + "/{projectId}/members", 123456789)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .content(objectMapper.writeValueAsString(addMemberRequest))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }

    @Test
    void addMemberToProject_should_throw404EntityNotFoundException_whenNewUserIsNotFound() throws Exception {
        Project project = setupProjectAndAddTestUser();

        AddMemberRequest addMemberRequest = new AddMemberRequest("randomuser@email.com");

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(addMemberRequest))
                                .header("Authorization", "Bearer " + loginAndGetToken()))
               .andExpect(status().isNotFound());
    }

    @Test
    void addMemberToProject_should_throw419IllegalStateException_whenNewUserIsAlreadyAMemberOfProject() throws Exception {
        Set<User> members = new HashSet<>();
        members.add(testUser);

        User newUserToAdd = User.builder()
                                .name("newUser")
                                .email("newEmail@email.com")
                                .password(passwordEncoder.encode("encodethis"))
                                .build();
        userRepository.save(newUserToAdd);
        members.add(newUserToAdd);

        Project project = Project.builder().name("projectName").members(members).build();
        projectRepository.save(project);

        AddMemberRequest addMemberRequest = new AddMemberRequest(newUserToAdd.getEmail());

        mockMvc.perform(post(basePath + "/{projectId}/members", project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + loginAndGetToken())
                                .content(objectMapper.writeValueAsString(addMemberRequest)))
               .andExpect(status().isConflict());
    }


    private Project setupProject() {
        Project project = Project.builder().name("projectName").members(new HashSet<>()).build();
        return projectRepository.save(project);
    }

    private Project setupProjectAndAddTestUser() {
        Project project = Project.builder().name("projectName").build();

        Set<User> members = new HashSet<>();
        members.add(testUser);
        project.setMembers(members);

        return projectRepository.save(project);
    }
}
