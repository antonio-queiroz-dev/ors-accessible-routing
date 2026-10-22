package br.labai.rotas.obstaculo;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;

@Entity
public class Obstaculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false)
    private double confianca;

    // Default para linhas gravadas antes de existir a coluna
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'BLOQUEADA'")
    private Passagem passagem;

    @Column(nullable = false)
    private OffsetDateTime detectadoEm;

    @Column(nullable = false)
    private OffsetDateTime recebidoEm;

    protected Obstaculo() {
    }

    public Obstaculo(double latitude, double longitude, String tipo, double confianca, Passagem passagem,
                     OffsetDateTime detectadoEm, OffsetDateTime recebidoEm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipo = tipo;
        this.confianca = confianca;
        this.passagem = passagem != null ? passagem : Passagem.BLOQUEADA;
        this.detectadoEm = detectadoEm;
        this.recebidoEm = recebidoEm;
    }

    public Long getId() { return id; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTipo() { return tipo; }
    public double getConfianca() { return confianca; }
    public Passagem getPassagem() { return passagem; }
    public OffsetDateTime getDetectadoEm() { return detectadoEm; }
    public OffsetDateTime getRecebidoEm() { return recebidoEm; }
}
