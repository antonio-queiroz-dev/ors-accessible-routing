package br.labai.rotas;

import br.labai.rotas.ors.OrsException;
import br.labai.rotas.rota.ParametroInvalidoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/** Transforma erros em respostas com mensagem legível. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> dadosInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return Map.of("erro", "Dados inválidos", "campos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> jsonIlegivel(HttpMessageNotReadableException e) {
        return Map.of("erro", "JSON inválido ou com campo no formato errado. Confira: texto em UTF-8, "
                + "números com ponto (ex.: -12.97) e data no formato ISO 8601 (ex.: 2026-10-21T13:00:00-03:00).");
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> parametroInvalido(ParametroInvalidoException e) {
        return Map.of("erro", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> parametroFaltando(MissingServletRequestParameterException e) {
        return Map.of("erro", "parâmetro obrigatório: " + e.getParameterName());
    }

    @ExceptionHandler(OrsException.class)
    public ResponseEntity<Map<String, Object>> erroDoOrs(OrsException e) {
        if (e.getCodigo() == OrsException.PONTO_NAO_ENCONTRADO) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Origem ou destino longe de qualquer rua ou caminho do mapa carregado."));
        }
        if (e.getCodigo() == OrsException.LIMITE_EXCEDIDO) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Origem e destino muito distantes: o limite é 100 km."));
        }
        if (e.getCodigo() == OrsException.ROTA_NAO_ENCONTRADA) {
            return ResponseEntity.unprocessableEntity().body(Map.of("erro",
                    "Não existe caminho entre a origem e o destino com esse perfil."));
        }
        log.warn("ORS recusou o pedido: {} {}", e.getCodigo(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("erro",
                "O motor de rotas recusou o pedido: " + e.getMessage()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> orsFora(ResourceAccessException e) {
        log.warn("ORS inacessível: {}", e.getMessage());
        return Map.of("erro", "Motor de rotas indisponível no momento. Tente de novo em instantes.");
    }
}
