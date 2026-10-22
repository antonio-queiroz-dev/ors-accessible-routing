package br.labai.rotas.obstaculo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
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
import tools.jackson.core.type.TypeReference;
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
                this.fixos = json.readValue(arquivo, new TypeReference<>() {});
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

    /** Para o /api/status. */
    public String estado() {
        if (url == null) {
            return "obstáculos de teste fixos (CAMERA_URL vazio)";
        }
        try {
            // Retângulo minúsculo: só confere se a câmera responde
            daCamera(0, 0, 0.0001, 0.0001);
            return "ok";
        } catch (CameraIndisponivelException e) {
            return "inacessível: " + e.getMessage();
        }
    }

    private List<Obstaculo> daCamera(double minLon, double minLat, double maxLon, double maxLat) {
        // bbox na ordem do GeoJSON: minLon,minLat,maxLon,maxLat
        // 6 casas (~10 cm) e sempre com ponto, nunca em notação científica
        String bbox = String.format(Locale.ROOT, "%.6f,%.6f,%.6f,%.6f", minLon, minLat, maxLon, maxLat);
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("bbox", bbox)
                .build().toUri();
        try {
            Obstaculo[] lista = camera.get().uri(uri).retrieve().body(Obstaculo[].class);
            return lista == null ? List.of() : Arrays.asList(lista);
        } catch (RestClientException e) {
            log.warn("Falha ao buscar obstáculos em {}: {}", uri, e.getMessage());
            throw new CameraIndisponivelException(e.getMessage());
        }
    }
}
