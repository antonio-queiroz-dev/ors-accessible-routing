package br.labai.rotas.ors;

/** Erro devolvido pelo ORS (ex.: 2009 rota não encontrada, 2010 ponto fora do mapa). */
public class OrsException extends RuntimeException {

    public static final int LIMITE_EXCEDIDO = 2004;
    public static final int ROTA_NAO_ENCONTRADA = 2009;
    public static final int PONTO_NAO_ENCONTRADO = 2010;

    private final int codigo;

    public OrsException(int codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
