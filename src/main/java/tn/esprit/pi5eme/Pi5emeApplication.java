package tn.esprit.pi5eme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Properties classes are registered via @ConfigurationPropertiesScan (not in SchedulingConfig,
// which is conditional and disabled in tests).
@SpringBootApplication
@ConfigurationPropertiesScan
public class Pi5emeApplication {

    public static void main(String[] args) {
        SpringApplication.run(Pi5emeApplication.class, args);
    }

}
