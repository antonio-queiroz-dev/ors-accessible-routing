package br.labai.rotas;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String TAG_ROTAS = "Rotas";
    public static final String TAG_OBSTACULOS = "Obstáculos";
    public static final String TAG_STATUS = "Status";

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de rotas acessíveis")
                        .version("1.0.0")
                        .description(DESCRICAO))
                .tags(List.of(
                        new Tag().name(TAG_ROTAS).description("Cálculo da rota, desviando dos obstáculos"),
                        new Tag().name(TAG_OBSTACULOS).description("Obstáculos conhecidos, para desenhar no mapa (só leitura)"),
                        new Tag().name(TAG_STATUS).description("Saúde da API e área atendida")));
    }

    private static final String DESCRICAO = """
            Calcula rotas para pessoas com mobilidade reduzida, principalmente cadeirantes, \
            desviando dos obstáculos que a câmera com visão computacional detectou nas calçadas.

            **[Página de teste com mapa](/)**: clique para marcar origem, destino e obstáculos simulados.

            ### Como funciona

            1. O front pede uma rota (`GET /api/rota`).
            2. A API pergunta ao back da câmera quais obstáculos existem perto do trajeto.
            3. Cada obstáculo vira um quadrado de 12 m em volta do ponto (cobre o erro do GPS) e vai para o \
            [openrouteservice](https://openrouteservice.org) como área a evitar. O perfil `wheelchair` do \
            openrouteservice já considera inclinação, meio-fio e tipo de piso quando o OpenStreetMap tem esses dados.
            4. A API devolve a rota em GeoJSON, com instruções em português e avisos.

            A API não guarda nada: os obstáculos ficam no back da câmera.

            ### Níveis de passagem

            | `passagem` | O que a rota faz |
            |---|---|
            | `bloqueada` | Sempre desvia. Sem desvio possível, devolve a rota direta com `desvioPossivel: false` e um aviso no ponto do bloqueio. |
            | `dificil` | Desvia se a rota aumentar até 30% ou até 100 m (o que for maior); senão passa e avisa. |
            | `contornavel` | Não desvia, só avisa ("Dá para contornar."). |

            Os avisos entram em `instrucoes`, logo depois do passo onde está o obstáculo, com `distanciaMetros: 0`.

            ### Erros

            Todo erro vem como `{ "erro": "mensagem" }`.

            | Status | Quando |
            |---|---|
            | 400 | Coordenada malformada, fora da área atendida, perfil inválido ou JSON inválido |
            | 422 | Não existe caminho com esse perfil (comum no `wheelchair` onde o mapa não tem dados de calçada) |
            | 502 | O openrouteservice recusou o pedido |
            | 503 | Câmera fora do ar, openrouteservice fora do ar ou limite de pedidos atingido |

            Coordenadas sempre em graus decimais com ponto. Na URL o formato é `lat,lon`; no GeoJSON \
            devolvido é `[longitude, latitude]`, como manda o padrão.""";
}
