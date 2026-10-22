package br.labai.rotas;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("API de rotas acessíveis")
                .version("0.1.0")
                .description("""
                        Rotas para pessoas com deficiência visual, usando os dados de acessibilidade \
                        do OpenStreetMap e desviando dos obstáculos detectados pela câmera.

                        Exemplo (Pelourinho, Salvador): origem `-12.9737,-38.5094`, destino `-12.9732,-38.5119`. \
                        Compare `considerarObstaculos=true` e `false` para ver o desvio."""));
    }
}
