package br.labai.rotas;

import br.labai.rotas.obstaculo.FonteObstaculos;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
public class StatusController {

    private static final Logger log = LoggerFactory.getLogger(StatusController.class);

    private final RestClient ors;
    private final FonteObstaculos obstaculos;

    public StatusController(RestClient orsRestClient, FonteObstaculos obstaculos) {
        this.ors = orsRestClient;
        this.obstaculos = obstaculos;
    }

    @Operation(summary = "Confere se a API está no ar e se consegue falar com o ORS e com o back da câmera")
    @GetMapping({"/api/status", "/health"})
    public Map<String, String> status() {
        return Map.of("api", "ok", "ors", estadoOrs(), "camera", obstaculos.estado());
    }

    /** Não pede rota: na API pública isso gastaria cota a cada verificação. */
    private String estadoOrs() {
        try {
            Map<?, ?> health = ors.get().uri("/v2/health").retrieve().body(Map.class);
            return health == null ? "sem resposta" : String.valueOf(health.get("status"));
        } catch (RestClientResponseException e) {
            // A API pública não tem /v2/health (404), mas responder já mostra que está no ar.
            // Outro status (ex.: 503 de um ORS próprio ainda carregando) é problema de verdade
            return e.getStatusCode().value() == 404 ? "ok" : "erro HTTP " + e.getStatusCode().value();
        } catch (ResourceAccessException e) {
            // Detalhe só no log: a mensagem traz endereços internos
            log.warn("ORS inacessível: {}", e.getMessage());
            return "inacessível";
        }
    }
}
