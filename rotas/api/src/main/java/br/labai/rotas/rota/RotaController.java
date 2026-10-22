package br.labai.rotas.rota;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RotaController {

    private final RotaService servico;
    private final List<String> perfis;

    public RotaController(RotaService servico, @Value("${rota.perfis}") List<String> perfis) {
        this.servico = servico;
        this.perfis = perfis;
    }

    @Operation(summary = "Calcula a rota a pé ou de cadeira de rodas, desviando dos obstáculos registrados")
    @GetMapping("/api/rota")
    public RotaResposta rota(
            @Parameter(description = "lat,lon", example = "-12.9737,-38.5094") @RequestParam String origem,
            @Parameter(description = "lat,lon", example = "-12.9732,-38.5119") @RequestParam String destino,
            @Parameter(description = "foot-walking ou wheelchair", example = "foot-walking")
            @RequestParam(defaultValue = "foot-walking") String perfil,
            @Parameter(description = "false para ver a rota sem desviar de nada (comparação)")
            @RequestParam(defaultValue = "true") boolean considerarObstaculos) {
        if (!perfis.contains(perfil)) {
            throw new ParametroInvalidoException("perfil deve ser um destes: " + String.join(", ", perfis));
        }
        return servico.calcular(Ponto.ler(origem, "origem"), Ponto.ler(destino, "destino"), perfil, considerarObstaculos);
    }
}
