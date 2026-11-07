package tn.esprit.pi5eme.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.pi5eme.entity.Role;
import tn.esprit.pi5eme.entity.User;
import tn.esprit.pi5eme.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed default Admin if not exists
        if (!userRepository.existsByEmail("admin@esprit.tn")) {
            User admin = User.builder()
                    .firstName("Super")
                    .lastName("Admin")
                    .email("admin@esprit.tn")
                    .password(passwordEncoder.encode("Admin@123456"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            userRepository.save(admin);
            log.info("Default Admin account created: admin@esprit.tn / Admin@123456");
        }

        // Seed default Regular User if not exists
        if (!userRepository.existsByEmail("user@esprit.tn")) {
            User user = User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("user@esprit.tn")
                    .password(passwordEncoder.encode("User@123456"))
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();
            userRepository.save(user);
            log.info("Default Regular User account created: user@esprit.tn / User@123456");
        }
    }
}

