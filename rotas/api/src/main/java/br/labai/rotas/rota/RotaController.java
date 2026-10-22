package br.labai.rotas.rota;

import br.labai.rotas.obstaculo.Obstaculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RotaController {

    /** Cada obstáculo vira um polígono no pedido ao ORS; acima disso o pedido fica grande demais. */
    private static final int MAXIMO_SIMULADOS = 50;

    private final RotaService servico;
    private final AreaAtendida area;
    private final List<String> perfis;

    public RotaController(RotaService servico, AreaAtendida area, @Value("${rota.perfis}") List<String> perfis) {
        this.servico = servico;
        this.area = area;
        this.perfis = perfis;
    }

    @Operation(summary = "Calcula a rota de cadeira de rodas ou a pé, desviando dos obstáculos da câmera")
    @GetMapping("/api/rota")
    public RotaResposta rota(
            @Parameter(description = "lat,lon", example = "-12.9737,-38.5094") @RequestParam String origem,
            @Parameter(description = "lat,lon", example = "-12.9732,-38.5119") @RequestParam String destino,
            @Parameter(description = "wheelchair ou foot-walking", example = "wheelchair")
            @RequestParam(defaultValue = "wheelchair") String perfil,
            @Parameter(description = "false para ver a rota sem desviar de nada (comparação)")
            @RequestParam(defaultValue = "true") boolean considerarObstaculos) {
        return calcular(Ponto.ler(origem, "origem"), Ponto.ler(destino, "destino"), perfil, considerarObstaculos, List.of());
    }

    @Operation(summary = "Igual ao GET, mas aceita obstáculos simulados no corpo (página de teste). Nada é guardado")
    @PostMapping("/api/rota")
    public RotaResposta rotaSimulada(@RequestBody RotaPedido pedido) {
        List<Obstaculo> simulados = pedido.obstaculos() == null ? List.of() : pedido.obstaculos();
        if (simulados.size() > MAXIMO_SIMULADOS) {
            throw new ParametroInvalidoException("no máximo " + MAXIMO_SIMULADOS + " obstáculos simulados por pedido");
        }
        simulados.forEach(o -> Ponto.verificar(new Ponto(o.latitude(), o.longitude()), "obstáculo"));
        return calcular(Ponto.verificar(pedido.origem(), "origem"), Ponto.verificar(pedido.destino(), "destino"),
                pedido.perfil() == null ? "wheelchair" : pedido.perfil(),
                pedido.considerarObstaculos() == null || pedido.considerarObstaculos(), simulados);
    }

    @Operation(summary = "Área atendida (minLon,minLat,maxLon,maxLat); bbox nulo quando não há limite")
    @GetMapping("/api/area")
    public Map<String, Object> area() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("nome", area.nome());
        resposta.put("bbox", area.bbox());
        return resposta;
    }

    private RotaResposta calcular(Ponto origem, Ponto destino, String perfil, boolean considerarObstaculos,
                                  List<Obstaculo> simulados) {
        if (!perfis.contains(perfil)) {
            throw new ParametroInvalidoException("perfil deve ser um destes: " + String.join(", ", perfis));
        }
        // Valida antes de chamar a câmera e o ORS: ponto fora da área não gasta cota
        area.validar(origem, "origem");
        area.validar(destino, "destino");
        return servico.calcular(origem, destino, perfil, considerarObstaculos, simulados);
    }
}
