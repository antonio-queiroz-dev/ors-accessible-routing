package br.labai.rotas.obstaculo;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** Dá para passar pela calçada onde está o obstáculo? Informado pela câmera. */
public enum Passagem {

    /** Não passa: a rota sempre contorna. */
    BLOQUEADA,
    /** Passa com dificuldade: a rota contorna se o desvio não aumentar muito o caminho. */
    DIFICIL,
    /** Dá para contornar ali mesmo: a rota não desvia, só avisa. */
    CONTORNAVEL;

    /** Valor no JSON: bloqueada, dificil ou contornavel. */
    public static final String VALORES = "bloqueada|dificil|contornavel";

    /** Converte o texto do JSON; se não vier, assume bloqueada. */
    public static Passagem de(String texto) {
        return texto == null ? BLOQUEADA : valueOf(texto.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }
}
