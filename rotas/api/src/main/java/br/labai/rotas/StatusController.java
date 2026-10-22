package br.labai.rotas;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class StatusController {

    private final RestClient ors;

    public StatusController(RestClient orsRestClient) {
        this.ors = orsRestClient;
    }

    @Operation(summary = "Confere se a API está no ar e se consegue falar com o ORS")
    @GetMapping("/api/status")
    public Map<String, String> status() {
        String estadoOrs;
        try {
            Map<?, ?> health = ors.get().uri("/v2/health").retrieve().body(Map.class);
            estadoOrs = String.valueOf(health.get("status"));
        } catch (Exception e) {
            estadoOrs = "inacessível: " + e.getMessage();
        }
        return Map.of("api", "ok", "ors", estadoOrs);
    }
}
