package app.momentos.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI momentosOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Momentos API")
                .version("0.1.0")
                .description("""
                        API da plataforma Momentos: grupos de amigos guardam fotos e vídeos
                        em uma linha do tempo, agrupados em Momentos com título.
                        """)
                .license(new License().name("MIT")));
    }
}
