package br.labai.rotas.rota;

import br.labai.rotas.ExemplosApi;
import br.labai.rotas.SwaggerConfig;
import br.labai.rotas.obstaculo.Obstaculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = SwaggerConfig.TAG_ROTAS)
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

    @Operation(summary = "Calcula a rota desviando dos obstáculos da câmera",
            description = "É o endpoint que o front usa. Gasta de 1 a 3 chamadas da cota do openrouteservice: "
                    + "uma para a rota direta e mais uma ou duas só se houver obstáculo no caminho.")
    @RespostasDeRota
    @GetMapping("/api/rota")
    public RotaResposta rota(
            @Parameter(description = "Ponto de partida, `lat,lon`", example = "-12.9737,-38.5094") @RequestParam String origem,
            @Parameter(description = "Ponto de chegada, `lat,lon`", example = "-12.9732,-38.5119") @RequestParam String destino,
            @Parameter(description = "`wheelchair` (cadeira de rodas) ou `foot-walking` (a pé)", example = "wheelchair")
            @RequestParam(defaultValue = "wheelchair") String perfil,
            @Parameter(description = "`false` devolve a rota sem desviar de nada, para comparar")
            @RequestParam(defaultValue = "true") boolean considerarObstaculos,
            @Parameter(description = "`true` devolve também `rotaSemDesvio`, sem gastar chamada extra do openrouteservice")
            @RequestParam(defaultValue = "false") boolean incluirRotaSemDesvio) {
        return calcular(Ponto.ler(origem, "origem"), Ponto.ler(destino, "destino"), perfil, considerarObstaculos,
                List.of(), incluirRotaSemDesvio);
    }

    @Operation(summary = "Igual ao GET, com obstáculos simulados no corpo",
            description = "Para testes e demonstração: os obstáculos do corpo valem só neste pedido, somados aos da câmera. "
                    + "Nada é guardado. A página de teste usa este endpoint.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "Bloqueio no caminho da cadeira", value = ExemplosApi.PEDIDO_SIMULADO)))
    @RespostasDeRota
    @PostMapping("/api/rota")
    public RotaResposta rotaSimulada(@RequestBody RotaPedido pedido) {
        List<Obstaculo> simulados = pedido.obstaculos() == null ? List.of() : pedido.obstaculos();
        if (simulados.size() > MAXIMO_SIMULADOS) {
            throw new ParametroInvalidoException("no máximo " + MAXIMO_SIMULADOS + " obstáculos simulados por pedido");
        }
        if (simulados.stream().anyMatch(Objects::isNull)) {
            throw new ParametroInvalidoException("obstaculos não pode ter itens nulos");
        }
        simulados.forEach(o -> Ponto.verificar(new Ponto(o.latitude(), o.longitude()), "obstáculo"));
        return calcular(Ponto.verificar(pedido.origem(), "origem"), Ponto.verificar(pedido.destino(), "destino"),
                pedido.perfil() == null ? "wheelchair" : pedido.perfil(),
                pedido.considerarObstaculos() == null || pedido.considerarObstaculos(), simulados,
                Boolean.TRUE.equals(pedido.incluirRotaSemDesvio()));
    }

    private RotaResposta calcular(Ponto origem, Ponto destino, String perfil, boolean considerarObstaculos,
                                  List<Obstaculo> simulados, boolean incluirRotaSemDesvio) {
        if (!perfis.contains(perfil)) {
            throw new ParametroInvalidoException("perfil deve ser um destes: " + String.join(", ", perfis));
        }
        // Valida antes de chamar a câmera e o ORS: ponto fora da área não gasta cota
        area.validar(origem, "origem");
        area.validar(destino, "destino");
        return servico.calcular(origem, destino, perfil, considerarObstaculos, simulados, incluirRotaSemDesvio);
    }
}
