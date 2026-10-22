package br.labai.rotas.obstaculo;

import br.labai.rotas.rota.AreaAtendida;
import br.labai.rotas.rota.ParametroInvalidoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Só leitura: repassa o que vem da câmera, para desenhar no mapa. A API não guarda obstáculos. */
@RestController
public class ObstaculoController {

    private final FonteObstaculos fonte;
    private final AreaAtendida area;

    public ObstaculoController(FonteObstaculos fonte, AreaAtendida area) {
        this.fonte = fonte;
        this.area = area;
    }

    @Operation(summary = "Obstáculos conhecidos dentro da área atendida (vindos da câmera, ou os de teste)")
    @GetMapping("/api/obstaculos")
    public List<Obstaculo> obstaculos(
            @Parameter(description = "minLon,minLat,maxLon,maxLat; padrão: a área atendida")
            @RequestParam(required = false) String bbox) {
        double[] b;
        try {
            b = bbox != null ? AreaAtendida.ler(bbox) : area.bbox();
        } catch (IllegalArgumentException e) {
            throw new ParametroInvalidoException("bbox deve ser minLon,minLat,maxLon,maxLat (ex.: -38.516,-12.983,-38.501,-12.964)");
        }
        if (b == null) {
            throw new ParametroInvalidoException("informe bbox: a área atendida não tem limite");
        }
        return fonte.dentro(b[0], b[1], b[2], b[3]);
    }
}
