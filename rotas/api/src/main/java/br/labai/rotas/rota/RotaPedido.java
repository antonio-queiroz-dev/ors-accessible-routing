package br.labai.rotas.rota;

import br.labai.rotas.obstaculo.Obstaculo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Corpo do POST /api/rota: igual ao GET, mais obstáculos simulados que não são guardados. */
public record RotaPedido(
        Ponto origem,
        Ponto destino,
        @Schema(description = "wheelchair (padrão) ou foot-walking")
        String perfil,
        @Schema(description = "false para ver a rota sem desviar de nada (comparação); padrão true")
        Boolean considerarObstaculos,
        @Schema(description = "Obstáculos simulados, somados aos da câmera só neste pedido")
        List<Obstaculo> obstaculos
) {
}
