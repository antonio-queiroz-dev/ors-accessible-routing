# API de rotas: resultados

Testado em 21/10/2026, com o ORS carregando Salvador (perfis `foot-walking` e `wheelchair`).

## Como subir e testar
No Git Bash, dentro de `rotas/`:
```bash
docker compose up -d --build
```
- Swagger: http://localhost:8080/swagger-ui.html
- Exemplo pronto: `GET /api/rota?origem=-12.9737,-38.5094&destino=-12.9732,-38.5119&perfil=foot-walking`
- Para ver a rota sem desvio, acrescente `&considerarObstaculos=false`.
- Para ver no mapa: arraste `testes/comparacao-pelourinho.geojson` para https://geojson.io. A linha vermelha é a rota original, a verde é o desvio e os quadrados são os obstáculos.

## Resultado de cada passo

**1. Projeto e Docker.** A API (Java 21, Spring Boot 3.5) roda no mesmo `docker-compose.yml` que o ORS. O ORS não tem mais porta publicada: só a API o enxerga, pela rede interna (`http://ors:8082`). `GET /api/status` devolve `{"api":"ok","ors":"ready"}`. As imagens base são multi-arquitetura e funcionam na VPS ARM.

**2. Obstáculos.** `POST /api/obstaculos` salva no H2, gravado num volume, e devolve 201. `GET /api/obstaculos` lista os registros. Os dados continuam lá depois de reiniciar o container. Entrada inválida devolve 400 com todos os erros de uma vez:
```json
{"erro":"Dados inválidos","campos":{"latitude":"deve estar entre -90 e 90","longitude":"deve estar entre -180 e 180","tipo":"é obrigatório","confianca":"deve estar entre 0 e 1"}}
```
O `tipo` é texto livre, até 100 caracteres. O `detectadoEm` é opcional: se a câmera não mandar, vale a hora em que a API recebeu.

**3. Obstáculos de teste.** Os 4 obstáculos ficam em `api/src/main/resources/obstaculos-teste.json` e são carregados se o banco estiver vazio e `OBSTACULOS_TESTE=true`:

| Tipo | Confiança | Onde |
|---|---|---|
| buraco | 0.90 | Rua da Oração |
| obra na calçada | 0.85 | Rua do Bispo |
| carro na calçada | 0.75 | Rua da Ajuda |
| lixo | 0.40 | Rua Guedes de Brito (abaixo de 0.6, deve ser ignorado) |

**4. Rota.** `GET /api/rota` devolve o formato do contrato, com instruções em português. Testei os erros de entrada e todos voltam com mensagem clara:

| Entrada | Resposta |
|---|---|
| perfil inválido | 400 `perfil deve ser um destes: foot-walking, wheelchair` |
| `origem=abc` | 400 `origem deve estar no formato lat,lon` |
| destino faltando | 400 `parâmetro obrigatório: destino` |
| ponto no mar | 400 `Origem ou destino longe de qualquer rua ou caminho do mapa carregado.` |
| São Paulo até Salvador | 400 `Origem e destino muito distantes: o limite é 100 km.` |
| ORS fora do ar | 503 `Motor de rotas indisponível no momento.` |

**5. Sem desvio possível.** Para testar, registrei um obstáculo de confiança 0.95 no último trecho da Ladeira da Misericórdia, que é a única chegada ao destino. A API devolveu a rota original (542 m) com `desvioPossivel: false` e `obstaculosEvitados: 0`. Esse teste foi feito num banco temporário. Depois voltei o banco para os 4 obstáculos de teste.

**6. Swagger.** Está ligado, com título, descrição e exemplos preenchidos (`/swagger-ui.html`). Basta clicar em "Try it out" e "Execute".

**7. Comparação** (Terreiro de Jesus até a Praça da Sé):

| | Distância | Tempo | Obstáculos evitados | Instruções |
|---|---|---|---|---|
| A pé, sem obstáculos | 542 m | 390 s | 0 | 7 |
| **A pé, com obstáculos** | **549 m** | **395 s** | **3** | 12 |
| Cadeira, sem obstáculos | 571 m | 411 s | 0 | 16 |
| Cadeira, com obstáculos | 571 m | 411 s | 0 | 16 |

Distância de cada obstáculo até a rota a pé:

| | buraco | obra | carro | lixo (0.4) |
|---|---|---|---|---|
| Sem obstáculos | 0 m | 0 m | 0 m | 0 m |
| Com obstáculos | 25,7 m | 45,1 m | 23,0 m | 2,5 m |

Instruções da rota original:
1. Siga para noroeste em direção a Largo do Cruzeiro do São Francisco (29 m)
2. Vire bastante à esquerda em direção a Rua da Oração (53 m)
3. Vire à direita em direção a Rua do Bispo (104 m)
4. Vire à esquerda em direção a Rua da Ajuda (100 m)
5. Vire à direita em direção a Rua Guedes de Brito (96 m)
6. Vire à direita em direção a Ladeira da Misericórdia (161 m)
7. Chegar a Ladeira da Misericórdia, à direita

Instruções com desvio:
1. Siga para noroeste em direção a Largo do Cruzeiro do São Francisco (29 m)
2. Continue em frente em direção a Largo Terreiro de Jesus (91 m)
3. Vire ligeiramente à esquerda em direção a Rua da Ajuda (30 m)
4. Vire à direita (13 m), à esquerda (8 m), à direita (39 m), à direita (72 m), à direita (42 m), à direita (11 m), à esquerda (56 m)
5. Vire à direita em direção a Ladeira da Misericórdia (158 m)
6. Chegar a Ladeira da Misericórdia, à direita

A resposta completa com desvio está em `testes/resposta-rota-com-desvio.json`.

## Explicação

**Como a rota é calculada.** A API não manda todos os obstáculos da cidade para o ORS. Primeiro ela pede a rota direta. Depois separa os obstáculos com confiança de 0.6 para cima que ficam até 500 m do trajeto, e vê quais estão a menos de ~8,5 m da linha da rota. Esse valor é a meia diagonal do quadrado de 12 m (era 4 m e ~2,8 m no primeiro teste), então qualquer ponto mais perto que isso pode cair dentro do quadrado. Se nenhum estiver no caminho, a rota direta já é a resposta, com uma chamada só ao ORS. Se algum estiver, ela pede de novo, mandando o quadrado de cada obstáculo próximo em `avoid_polygons`. Assim `obstaculosEvitados` conta só os obstáculos que realmente estavam no caminho.

**O que a comparação mostra.** A pé, os 3 obstáculos confiáveis estavam em cima da rota, a 0 m. Com o desvio, o mais próximo ficou a 23 m, e a rota cresceu só 7 m: o ORS cortou pelo Terreiro de Jesus e por passagens internas em vez de pegar a Rua da Oração e a Rua do Bispo. O lixo (0.4) continua a 2,5 m da rota, o que prova que o filtro de confiança funciona. Na cadeira de rodas nada mudou porque esse perfil já evita aquelas ruas, por causa do piso e da inclinação no OSM, e passa longe dos 3 obstáculos.

**Um caso que eu não esperava.** No teste do passo 5, o ORS não disse "sem rota". Ele encaixou o destino em outro caminho a 17 m do ponto pedido e devolveu uma rota de 399 m, como se fosse um desvio. Para quem não enxerga, ser levado a outro lugar sem aviso é pior do que receber `desvioPossivel: false`. Por isso a API compara o começo e o fim do desvio com os da rota direta: se algum se afastar mais de 10 m do ponto pedido, o desvio é descartado. O caso de o ORS responder "rota não encontrada" (erro 2009) também cai em `desvioPossivel: false`. Esse caminho está no código, mas não consegui provocá-lo neste mapa.

**Decisões menores.**
- Os parâmetros da rota vêm do `.env`: `ROTA_CONFIANCA_MINIMA=0.6` e `ROTA_LADO_QUADRADO_METROS=4`.
- O ORS escreve "nordoeste" em vez de "noroeste". A API corrige isso nas instruções, que é o que o leitor de tela fala.
- As datas saem no fuso de Salvador (`-03:00`), configurável por `API_FUSO`.
- Acrescentei o parâmetro opcional `considerarObstaculos` (padrão `true`), que não estava no contrato. Ele serve para a demonstração e não muda nada para quem não o usa.

**Limitações para conversar com a equipe.**
- **Instruções sem nome de rua.** O desvio passa por calçadas e passagens sem nome no OSM, e o ORS gera "Vire à direita" várias vezes seguidas (o passo 4 das instruções com desvio tem 7 viradas). Para o leitor de tela isso é fraco. Depois dá para juntar viradas curtas ou dizer "siga pela passagem".
- **Precisão do GPS.** O erro típico do GPS do celular fica entre 5 e 10 m. O quadrado passou de 4 m para 12 m por isso (ver "Passagem dos obstáculos"). Ainda vale testar com dados reais da câmera.
- **Encoding do terminal.** Um POST feito no Git Bash com acento ("corrimão") chegou fora de UTF-8 e foi recusado com 400. Pelo Swagger ou pelo Python da câmera isso não acontece.

## Passagem dos obstáculos (21/10/2026, noite)

O `POST /api/obstaculos` ganhou o campo opcional `passagem`, que responde "dá para passar pela calçada aqui?". Se não vier, vale `bloqueada`. Outro valor devolve 400: `{"campos":{"passagem":"deve ser bloqueada, dificil ou contornavel"}}`. A rota continua usando só `avoid_polygons`:

| `passagem` | Como a rota trata | Aviso nas instruções, se a rota passar por ele |
|---|---|---|
| `bloqueada` | sempre contorna; sem desvio, `desvioPossivel: false` | nenhum |
| `dificil` | contorna se o desvio aumentar a rota em até 30% ou em até 100 m, o que for maior | `Atenção: buraco em cerca de X metros. Passagem difícil, sem desvio próximo.` |
| `contornavel` | nunca desvia | `Atenção: buraco em cerca de X metros. Dá para contornar.` |

- **Limite do desvio:** relativo ao tamanho da rota (`ROTA_DESVIO_PERCENTUAL=30`), com piso para rotas curtas (`ROTA_DESVIO_PISO_METROS=100`). Numa rota de 342 m o limite é 103 m; numa de 2,3 km, 691 m.
- **Quadrado de 12 m** (`ROTA_LADO_QUADRADO_METROS`), antes 4 m. Com 4 m, um obstáculo só contava como "no caminho" se o GPS errasse menos de 2,8 m; com 12 m a folga é de 8,5 m. O ORS bloqueia o trecho de rua inteiro que encosta no quadrado, e perto de esquina pode pegar a rua transversal também.
- **Aviso:** entra logo depois da instrução do passo em que o obstáculo está, com `distanciaMetros: 0`. X é a distância desde o começo desse passo, arredondada de 5 em 5 m.
- **Checagem de 10 m** da saída e da chegada vale para os dois desvios. Em `dificil`, se o desvio for descartado por isso, a rota passa pelo obstáculo com aviso.
- **Ordem:** o desvio da passagem difícil é comparado com a rota que já contorna as bloqueadas. Várias passagens difíceis são evitadas juntas ou nenhuma.
- **`obstaculosEvitados`** conta os obstáculos do caminho direto que ficaram fora da rota devolvida.
- **Docker:** o `docker-compose.yml` agora repassa as variáveis `ROTA_*` para a API (antes elas eram ignoradas no container).

**Testes** (um obstáculo por vez, API temporária com banco descartável):

| Cenário | `bloqueada` | `dificil` | `contornavel` |
|---|---|---|---|
| Rota longa (2.302 m), ponto A (-12.976554,-38.513003) | 2.274 m, evitado | 2.274 m, evitado | 2.302 m, aviso |
| Rota longa, ponto B (-12.98153,-38.515991) | 2.282 m, evitado | 2.282 m, evitado | 2.302 m, aviso |
| Rota longa, ponto D (-12.982601,-38.518992), desvio +350 m | 2.652 m, evitado | 2.652 m, evitado (limite 691 m) | não testado |
| Rota curta (342 m), ponto E (-12.973802,-38.511704), desvio +136 m | 478 m, evitado | 342 m, desvio recusado (limite 103 m), aviso | 342 m, aviso |
| Pelourinho, chegada bloqueada | 542 m, `desvioPossivel: false` | 542 m, aviso | 542 m, aviso |

Rota curta: origem `-12.973652,-38.510413`, destino `-12.973431,-38.512221`. Também: sem `passagem` igual a `bloqueada`; A bloqueada e B difícil juntos, 2.274 m e 2 evitados. No banco real, Pelourinho 549 m (3 evitados) e rota longa 2.309 m (1 evitado).

**Por que não o custom_model do ORS.** Foi testado com áreas e `priority multiply_by`. Funciona, mas no `foot-walking` qualquer `custom_model`, até vazio, troca a ponderação base do perfil: a rota longa (Terreiro de Jesus até o Campo Grande) passa de 2.302 m para 2.683 m e deixa de usar a Av. Lafayete Coutinho (`road_class` PRIMARY, de 396 m para 67 m). Testado sem sucesso: `preference` "recommended" e "custom", `distance_influence` 0, 50, 100 e 200, regras de `speed` (ignoradas pelo ORS) e de `priority` (só penalizam, não desfazem a penalidade da via principal). O `enable_custom_models` foi desligado de novo no `ors/ors-config.yml`.

## Arquivos
- `api/`: projeto Spring Boot (`Dockerfile`, `pom.xml`, código em `src/`).
- `api/src/test/.../GeoTest.java` e `PassagemTest.java`: quadrado, distância e posição na rota, conversão de `passagem` (6 testes, passando).
- `docker-compose.yml`: serviços `ors` (sem porta) e `api` (porta `API_PORTA`).
- `docker-compose.medir.yml`: expõe o ORS em `127.0.0.1:8082`, usado pelo `medir.sh`.
- `testes/comparacao-pelourinho.geojson`: as duas rotas e os obstáculos, para ver no mapa.
- Novas variáveis no `.env`: `API_PORTA`, `API_JAVA_OPTS`, `API_FUSO`, `OBSTACULOS_TESTE`, `ROTA_LADO_QUADRADO_METROS`, `ROTA_DESVIO_PERCENTUAL`, `ROTA_DESVIO_PISO_METROS`. Opcionais, com padrão no código: `ROTA_CONFIANCA_MINIMA`, `DB_USUARIO`, `DB_SENHA`.
