package br.labai.rotas.rota;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Resposta do GET /api/rota, no formato do contrato. */
public record RotaResposta(
        long distanciaMetros,
        long duracaoSegundos,
        Geometria geometria,
        List<Instrucao> instrucoes,
        @Schema(description = "Obstáculos que estavam no caminho direto e ficaram fora da rota devolvida")
        int obstaculosEvitados,
        @Schema(description = "false quando não há caminho que evite os obstáculos de passagem bloqueada; a rota devolvida passa por eles")
        boolean desvioPossivel
) {

    /** GeoJSON LineString, coordenadas em [longitude, latitude]. */
    public record Geometria(String type, List<double[]> coordinates) {
    }

    public record Instrucao(String texto, long distanciaMetros) {
    }
}
