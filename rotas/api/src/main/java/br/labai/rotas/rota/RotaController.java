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
    private final AreaAtendida area;
    private final List<String> perfis;

    public RotaController(RotaService servico, AreaAtendida area, @Value("${rota.perfis}") List<String> perfis) {
        this.servico = servico;
        this.area = area;
        this.perfis = perfis;
    }

    @Operation(summary = "Calcula a rota a pé ou de cadeira de rodas, desviando dos obstáculos registrados")
    @GetMapping("/api/rota")
    public RotaResposta rota(
            @Parameter(description = "lat,lon", example = "-12.9737,-38.5094") @RequestParam String origem,
            @Parameter(description = "lat,lon", example = "-12.9732,-38.5119") @RequestParam String destino,
            @Parameter(description = "wheelchair ou foot-walking", example = "wheelchair")
            @RequestParam(defaultValue = "wheelchair") String perfil,
            @Parameter(description = "false para ver a rota sem desviar de nada (comparação)")
            @RequestParam(defaultValue = "true") boolean considerarObstaculos) {
        if (!perfis.contains(perfil)) {
            throw new ParametroInvalidoException("perfil deve ser um destes: " + String.join(", ", perfis));
        }
        Ponto a = Ponto.ler(origem, "origem");
        Ponto b = Ponto.ler(destino, "destino");
        // Valida antes de chamar a câmera e o ORS: ponto fora da área não gasta cota
        area.validar(a, "origem");
        area.validar(b, "destino");
        return servico.calcular(a, b, perfil, considerarObstaculos);
    }
}
