package app.momentos.moments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MomentAssignmentConfig {

    @Bean
    public MomentAssignmentService momentAssignmentService() {
        return new MomentAssignmentService();
    }
}
