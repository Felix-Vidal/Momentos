package app.momentos.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Infraestrutura")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Verificação rápida de que a API está no ar")
    public Map<String, Object> ping() {
        return Map.of("status", "ok", "timestamp", Instant.now());
    }
}
