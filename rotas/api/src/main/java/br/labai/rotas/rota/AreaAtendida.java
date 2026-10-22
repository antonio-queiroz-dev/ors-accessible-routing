package br.labai.rotas.rota;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Retângulo onde a API calcula rotas (área piloto). AREA_BBOX vazio desliga a checagem. */
@Component
public class AreaAtendida {

    private final String nome;
    private final double[] bbox;

    public AreaAtendida(@Value("${area.bbox:}") String bbox, @Value("${area.nome:}") String nome) {
        this.nome = nome;
        this.bbox = bbox.isBlank() ? null : ler(bbox);
    }

    /** minLon,minLat,maxLon,maxLat, como no GeoJSON e no CIDADE_BBOX. */
    static double[] ler(String texto) {
        String[] partes = texto.split(",");
        if (partes.length != 4) {
            throw new IllegalArgumentException("AREA_BBOX deve ser minLon,minLat,maxLon,maxLat: " + texto);
        }
        double[] b = new double[4];
        for (int i = 0; i < 4; i++) {
            b[i] = Double.parseDouble(partes[i].trim());
        }
        if (b[0] >= b[2] || b[1] >= b[3]) {
            throw new IllegalArgumentException("AREA_BBOX com mínimo maior que máximo: " + texto);
        }
        return b;
    }

    void validar(Ponto p, String nomeParametro) {
        if (bbox != null && (p.longitude() < bbox[0] || p.latitude() < bbox[1]
                || p.longitude() > bbox[2] || p.latitude() > bbox[3])) {
            throw new ParametroInvalidoException(nomeParametro + " fora da área atendida"
                    + (nome.isBlank() ? "" : " (" + nome + ")") + ".");
        }
    }
}
