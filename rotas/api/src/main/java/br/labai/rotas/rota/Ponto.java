package br.labai.rotas.rota;

/** Ponto recebido na API como "lat,lon". */
public record Ponto(double latitude, double longitude) {

    static Ponto ler(String texto, String nomeParametro) {
        String[] partes = texto == null ? new String[0] : texto.split(",");
        if (partes.length != 2) {
            throw new ParametroInvalidoException(nomeParametro + " deve estar no formato lat,lon (ex.: -12.9737,-38.5094)");
        }
        double lat, lon;
        try {
            lat = Double.parseDouble(partes[0].trim());
            lon = Double.parseDouble(partes[1].trim());
        } catch (NumberFormatException e) {
            throw new ParametroInvalidoException(nomeParametro + " deve ter números com ponto (ex.: -12.9737,-38.5094)");
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new ParametroInvalidoException(nomeParametro + ": latitude deve estar entre -90 e 90 e longitude entre -180 e 180");
        }
        return new Ponto(lat, lon);
    }

    /** Formato do ORS e do GeoJSON: [longitude, latitude]. */
    double[] lonLat() {
        return new double[] {longitude, latitude};
    }
}
