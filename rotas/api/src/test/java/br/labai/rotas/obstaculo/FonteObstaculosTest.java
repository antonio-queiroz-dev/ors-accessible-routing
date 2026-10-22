package br.labai.rotas.obstaculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FonteObstaculosTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void descartaRegistrosRuinsSemDerrubarOsBons() {
        List<Obstaculo> lista = FonteObstaculos.converter(json.readTree("""
                [ null,
                  {"lat": -12.97, "lng": -38.51},
                  {"latitude": "x", "longitude": -38.51},
                  {"latitude": 95, "longitude": -38.51},
                  {"id": 7, "latitude": -12.97, "longitude": -38.51, "passagem": "dificil", "registradoEm": "2026-10-22T14:30:00Z"} ]
                """));
        assertEquals(1, lista.size());
        assertEquals(Passagem.DIFICIL, lista.get(0).passagem());
    }

    @Test
    void passagemDesconhecidaViraBloqueada() {
        List<Obstaculo> lista = FonteObstaculos.converter(json.readTree("""
                [{"latitude": -12.97, "longitude": -38.51, "passagem": "talvez"}]
                """));
        assertEquals(Passagem.BLOQUEADA, lista.get(0).passagem());
    }

    @Test
    void camposOpcionaisAusentes() {
        Obstaculo o = FonteObstaculos.converter(json.readTree("""
                [{"latitude": -12.97, "longitude": -38.51}]
                """)).get(0);
        assertEquals(Passagem.BLOQUEADA, o.passagem());
        assertEquals("obstáculo", o.tipo());
        assertNull(o.confianca());
    }
}
