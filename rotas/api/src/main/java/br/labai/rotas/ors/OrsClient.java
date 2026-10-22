package br.labai.rotas.ors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Chama o endpoint de direções do ORS e devolve só o que a API usa. */
@Component
public class OrsClient {

    private final RestClient ors;
    private final ObjectMapper json;

    public OrsClient(RestClient orsRestClient, ObjectMapper json) {
        this.ors = orsRestClient;
        this.json = json;
    }

    /**
     * @param evitar anéis fechados em [lon, lat] para evitar; lista vazia para nenhum
     */
    public RotaOrs rota(String perfil, double[] origem, double[] destino, List<List<double[]>> evitar) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("coordinates", List.of(origem, destino));
        corpo.put("instructions", true);
        corpo.put("language", "pt");
        if (!evitar.isEmpty()) {
            // MultiPolygon: cada polígono é uma lista de anéis; aqui só o anel externo
            List<List<List<double[]>>> multi = evitar.stream().map(List::of).toList();
            corpo.put("options", Map.of("avoid_polygons", Map.of("type", "MultiPolygon", "coordinates", multi)));
        }

        JsonNode resposta;
        try {
            resposta = ors.post()
                    .uri("/v2/directions/{perfil}/geojson", perfil)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            throw erroDoOrs(e);
        }
        return ler(resposta);
    }

    private OrsException erroDoOrs(HttpClientErrorException e) {
        try {
            JsonNode erro = json.readTree(e.getResponseBodyAsString()).path("error");
            return new OrsException(erro.path("code").asInt(), erro.path("message").asText(e.getMessage()));
        } catch (Exception ignorada) {
            return new OrsException(0, e.getMessage());
        }
    }

    private static RotaOrs ler(JsonNode resposta) {
        JsonNode rota = resposta.path("features").path(0);
        JsonNode resumo = rota.path("properties").path("summary");

        List<double[]> coordenadas = new ArrayList<>();
        for (JsonNode c : rota.path("geometry").path("coordinates")) {
            coordenadas.add(new double[] {c.get(0).asDouble(), c.get(1).asDouble()});
        }

        List<RotaOrs.Passo> passos = new ArrayList<>();
        for (JsonNode segmento : rota.path("properties").path("segments")) {
            for (JsonNode passo : segmento.path("steps")) {
                passos.add(new RotaOrs.Passo(passo.path("instruction").asText(), passo.path("distance").asDouble(),
                        passo.path("way_points").path(0).asInt()));
            }
        }
        return new RotaOrs(resumo.path("distance").asDouble(), resumo.path("duration").asDouble(), coordenadas, passos);
    }
}
