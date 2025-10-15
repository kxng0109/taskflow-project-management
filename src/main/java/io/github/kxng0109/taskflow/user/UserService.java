package io.github.kxng0109.taskflow.user;

import io.github.kxng0109.taskflow.security.dto.RegistrationRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegistrationRequest registrationRequest) {
        String userEmail = registrationRequest.email();
        String userPassword = registrationRequest.password();
        String userName = registrationRequest.name();

        boolean userExists = userRepository.findByEmail(userEmail).isPresent();
        if (userExists) {
            throw new IllegalStateException("User with that email already exists.");
        }

        String hashedPassword = passwordEncoder.encode(userPassword);

        User newUser = User.builder()
                .email(userEmail)
                .password(hashedPassword)
                .name(userName)
                .build();

        return userRepository.save(newUser);
    }
}
