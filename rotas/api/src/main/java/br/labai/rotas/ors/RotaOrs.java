package br.labai.rotas.ors;

import java.util.List;

/** O que interessa da resposta do ORS. Coordenadas em [longitude, latitude]. */
public record RotaOrs(double distanciaMetros, double duracaoSegundos, List<double[]> coordenadas,
                      List<Passo> passos) {

    /** @param inicio índice em coordenadas onde o passo começa */
    public record Passo(String instrucao, double distanciaMetros, int inicio) {
    }
}
