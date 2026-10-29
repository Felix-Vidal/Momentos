package app.momentos.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base dos testes `*IT`: contexto completo, porta aleatória e PostgreSQL em container.
 *
 * <p>Sem Docker na máquina os testes são ignorados em vez de falharem, para o `verify`
 * continuar utilizável em ambientes sem container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ContainersConfig.class)
@EnabledIfDockerAvailable
@ActiveProfiles("test")
public @interface IntegrationTest {
}
