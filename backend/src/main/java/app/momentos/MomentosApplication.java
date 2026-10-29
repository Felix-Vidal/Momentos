package app.momentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MomentosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MomentosApplication.class, args);
    }
}
