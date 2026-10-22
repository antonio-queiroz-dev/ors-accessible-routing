package br.labai.rotas.obstaculo;

/**
 * Obstáculo como vem do back da câmera. Só latitude, longitude e passagem importam para a rota;
 * tipo entra no texto do aviso e confianca, se vier, filtra detecções duvidosas.
 */
public record Obstaculo(double latitude, double longitude, Passagem passagem, String tipo, Double confianca) {

    public Obstaculo {
        // Sem passagem informada, o mais seguro é tratar como bloqueada
        passagem = passagem != null ? passagem : Passagem.BLOQUEADA;
        tipo = tipo != null && !tipo.isBlank() ? tipo : "obstáculo";
    }
}
