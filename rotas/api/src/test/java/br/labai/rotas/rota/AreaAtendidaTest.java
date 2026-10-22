package br.labai.rotas.rota;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AreaAtendidaTest {

    private final AreaAtendida pelourinho = new AreaAtendida("-38.5160,-12.9830,-38.5010,-12.9640", "Pelourinho");

    @Test
    void aceitaPontoDentro() {
        assertDoesNotThrow(() -> pelourinho.validar(new Ponto(-12.9737, -38.5094), "origem"));
    }

    @Test
    void recusaPontoForaComNomeDaArea() {
        var erro = assertThrows(ParametroInvalidoException.class,
                () -> pelourinho.validar(new Ponto(-12.9000, -38.5094), "destino"));
        assertEquals("destino fora da área atendida (Pelourinho).", erro.getMessage());
    }

    @Test
    void semBboxNaoValida() {
        assertDoesNotThrow(() -> new AreaAtendida("", "").validar(new Ponto(-23.5, -46.6), "origem"));
    }

    @Test
    void recusaBboxInvertido() {
        assertThrows(IllegalArgumentException.class, () -> AreaAtendida.ler("-38.50,-12.98,-38.51,-12.96"));
    }
}
