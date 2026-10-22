package br.labai.rotas;

import br.labai.rotas.obstaculo.FonteObstaculos;
import br.labai.rotas.rota.AreaAtendida;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
@Tag(name = SwaggerConfig.TAG_STATUS)
public class StatusController {

    private static final Logger log = LoggerFactory.getLogger(StatusController.class);

    private final RestClient ors;
    private final FonteObstaculos obstaculos;
    private final AreaAtendida area;

    public StatusController(RestClient orsRestClient, FonteObstaculos obstaculos, AreaAtendida area) {
        this.ors = orsRestClient;
        this.obstaculos = obstaculos;
        this.area = area;
    }

    @Operation(summary = "Saúde da API, do openrouteservice e do back da câmera",
            description = "Não pede rota ao openrouteservice, para não gastar cota. "
                    + "`camera` diz `obstáculos de teste fixos` quando `CAMERA_URL` está vazio.")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = ExemplosApi.HEALTH)))
    @GetMapping({"/api/status", "/health"})
    public Map<String, String> status() {
        return Map.of("api", "ok", "ors", estadoOrs(), "camera", obstaculos.estado());
    }

    @Operation(summary = "Área atendida",
            description = "`bbox` em `[minLon, minLat, maxLon, maxLat]`, ou `null` quando não há limite (mundo todo). "
                    + "Origem ou destino fora dela dão 400.")
    @GetMapping("/api/area")
    public Map<String, Object> area() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("nome", area.nome());
        resposta.put("bbox", area.bbox());
        return resposta;
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
