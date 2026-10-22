package br.labai.rotas.obstaculo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Corpo do POST /api/obstaculos, enviado pela câmera. */
public record NovoObstaculo(

        @Schema(example = "-12.9725")
        @NotNull(message = "é obrigatória")
        @DecimalMin(value = "-90", message = "deve estar entre -90 e 90")
        @DecimalMax(value = "90", message = "deve estar entre -90 e 90")
        Double latitude,

        @Schema(example = "-38.5098")
        @NotNull(message = "é obrigatória")
        @DecimalMin(value = "-180", message = "deve estar entre -180 e 180")
        @DecimalMax(value = "180", message = "deve estar entre -180 e 180")
        Double longitude,

        @Schema(example = "buraco", description = "Texto livre por enquanto")
        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String tipo,

        @Schema(example = "0.87")
        @NotNull(message = "é obrigatória")
        @DecimalMin(value = "0", message = "deve estar entre 0 e 1")
        @DecimalMax(value = "1", message = "deve estar entre 0 e 1")
        Double confianca,

        @Schema(example = "bloqueada", allowableValues = {"bloqueada", "dificil", "contornavel"},
                description = "Dá para passar pela calçada? bloqueada: não passa; dificil: passa com dificuldade; "
                        + "contornavel: dá para contornar ali mesmo. Opcional: se não vier, vale bloqueada")
        @Pattern(regexp = Passagem.VALORES, message = "deve ser bloqueada, dificil ou contornavel")
        String passagem,

        @Schema(example = "2026-10-21T13:00:00-03:00",
                description = "Opcional: se não vier, usa a hora em que a API recebeu")
        OffsetDateTime detectadoEm
) {
}
