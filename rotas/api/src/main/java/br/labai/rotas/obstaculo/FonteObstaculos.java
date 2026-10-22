package br.labai.rotas.obstaculo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * De onde vêm os obstáculos. A API não guarda nada: a cada rota pergunta ao back da câmera
 * (CAMERA_URL) o que há dentro do retângulo. Com CAMERA_URL vazio, usa os obstáculos de
 * teste fixos de obstaculos-teste.json.
 */
@Component
public class FonteObstaculos {

    private static final Logger log = LoggerFactory.getLogger(FonteObstaculos.class);

    private final String url;
    private final RestClient camera;
    private final List<Obstaculo> fixos;

    public FonteObstaculos(@Value("${camera.url:}") String url, ObjectMapper json) throws IOException {
        this.url = url.isBlank() ? null : url.trim();
        var tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(Duration.ofSeconds(3));
        tempos.setReadTimeout(Duration.ofSeconds(5));
        this.camera = RestClient.builder().requestFactory(tempos).build();
        if (this.url == null) {
            try (InputStream arquivo = new ClassPathResource("obstaculos-teste.json").getInputStream()) {
                this.fixos = converter(json.readTree(arquivo));
            }
            log.info("CAMERA_URL vazio: usando {} obstáculos de teste fixos", fixos.size());
        } else {
            this.fixos = List.of();
            log.info("Obstáculos vêm de {}", this.url);
        }
    }

    /** Obstáculos dentro do retângulo (graus). */
    public List<Obstaculo> dentro(double minLon, double minLat, double maxLon, double maxLat) {
        List<Obstaculo> todos = url == null ? fixos : daCamera(minLon, minLat, maxLon, maxLat);
        // Filtra aqui também, caso a câmera ignore o bbox e devolva tudo
        return todos.stream()
                .filter(o -> o.latitude() >= minLat && o.latitude() <= maxLat
                        && o.longitude() >= minLon && o.longitude() <= maxLon)
                .toList();
    }

    /** Para o /health. O motivo da falha fica só no log, para não expor endereços internos. */
    public String estado() {
        if (url == null) {
            return "obstáculos de teste fixos (CAMERA_URL vazio)";
        }
        try {
            // Retângulo minúsculo: só confere se a câmera responde
            daCamera(0, 0, 0.0001, 0.0001);
            return "ok";
        } catch (CameraIndisponivelException e) {
            return "inacessível";
        }
    }

    private List<Obstaculo> daCamera(double minLon, double minLat, double maxLon, double maxLat) {
        // bbox na ordem do GeoJSON: minLon,minLat,maxLon,maxLat
        // 6 casas (~10 cm) e sempre com ponto, nunca em notação científica
        String bbox = String.format(Locale.ROOT, "%.6f,%.6f,%.6f,%.6f", minLon, minLat, maxLon, maxLat);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("bbox", bbox)
                .build().toUri();
        JsonNode lista;
        try {
            lista = camera.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Falha ao buscar obstáculos em {}: {}", uri, e.getMessage());
            throw new CameraIndisponivelException(e.getMessage());
        }
        if (lista != null && !lista.isArray()) {
            log.warn("Resposta de {} não é uma lista: {}", uri, abreviar(lista));
            throw new CameraIndisponivelException("a resposta da câmera não é uma lista");
        }
        return lista == null ? List.of() : converter(lista);
    }

    /**
     * Converte item a item: um registro ruim é descartado (com log) em vez de derrubar todas as rotas.
     * Sem coordenadas não há como usar o obstáculo; passagem desconhecida vale como bloqueada (o lado seguro).
     */
    static List<Obstaculo> converter(JsonNode lista) {
        List<Obstaculo> obstaculos = new ArrayList<>();
        for (JsonNode item : lista) {
            JsonNode lat = item.path("latitude");
            JsonNode lon = item.path("longitude");
            if (!lat.isNumber() || !lon.isNumber() || !Double.isFinite(lat.asDouble()) || !Double.isFinite(lon.asDouble())
                    || Math.abs(lat.asDouble()) > 90 || Math.abs(lon.asDouble()) > 180) {
                log.warn("Obstáculo descartado, sem latitude e longitude válidas: {}", abreviar(item));
                continue;
            }
            Passagem passagem;
            String textoPassagem = item.path("passagem").isString() ? item.path("passagem").asString() : null;
            try {
                passagem = Passagem.de(textoPassagem);
            } catch (IllegalArgumentException e) {
                log.warn("Passagem desconhecida \"{}\", tratada como bloqueada: {}", textoPassagem, abreviar(item));
                passagem = Passagem.BLOQUEADA;
            }
            String tipo = item.path("tipo").isString() ? item.path("tipo").asString() : null;
            Double confianca = item.path("confianca").isNumber() ? item.path("confianca").asDouble() : null;
            obstaculos.add(new Obstaculo(lat.asDouble(), lon.asDouble(), passagem, tipo, confianca));
        }
        return obstaculos;
    }

    private static String abreviar(JsonNode no) {
        String texto = no.toString();
        return texto.length() > 200 ? texto.substring(0, 200) + "..." : texto;
    }
}
