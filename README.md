# Rotas acessíveis

API de rotas para pessoas com mobilidade reduzida, principalmente cadeirantes, que desvia dos obstáculos detectados nas calçadas por uma câmera com visão computacional. Área piloto: Pelourinho, Salvador (BA). Parte do projeto da Equipe 6 no Hackathon LAB.AI 2026.

- **API no ar:** https://rotas.antonioqueiroz.dev
- **Documentação (Swagger):** https://rotas.antonioqueiroz.dev/swagger-ui.html
- **Página de teste com mapa:** https://rotas.antonioqueiroz.dev/

## Como funciona

1. O front pede uma rota com origem e destino.
2. A API calcula a rota no [openrouteservice](https://openrouteservice.org), perfil cadeira de rodas, que já considera inclinação, meio-fio e tipo de piso quando o OpenStreetMap tem esses dados.
3. A API consulta o back da câmera e pega os obstáculos da área. Os que estão no caminho viram áreas a evitar, e a rota é recalculada.
4. A resposta traz a linha da rota em GeoJSON, as instruções em português e os avisos.

A API não guarda nada: os obstáculos ficam no back da câmera e são lidos na hora de cada rota.

**Dois motores de rota.** Dentro de Salvador, a API usa um openrouteservice próprio na VPS, com o mapa da cidade e sem limite de uso. Fora de Salvador, usa a API pública do openrouteservice (2.000 rotas por dia, 40 por minuto). Se um deles cair ou ficar sem cota, o outro assume onde puder. O campo `motor` da resposta diz quem calculou.

| `passagem` do obstáculo | O que a rota faz |
|---|---|
| `bloqueada` | Sempre desvia. Sem caminho alternativo, responde `desvioPossivel: false` e avisa onde está o bloqueio. |
| `dificil` | Desvia se a rota aumentar até 30% ou até 100 m; senão passa e avisa. |
| `contornavel` | Não desvia, só avisa. |

## Exemplo

```
GET https://rotas.antonioqueiroz.dev/api/rota?origem=-12.9738,-38.5123&destino=-12.9713,-38.5083
```

```json
{
  "distanciaMetros": 642,
  "duracaoSegundos": 463,
  "geometria": { "type": "LineString", "coordinates": [[-38.51231, -12.97378], [-38.51214, -12.97391]] },
  "instrucoes": [
    { "texto": "Siga para sul em direção a Ladeira da Misericórdia", "distanciaMetros": 53 },
    { "texto": "Vire à esquerda em direção a Rua Portas do Carmo", "distanciaMetros": 245 },
    { "texto": "Atenção: degrau em cerca de 20 metros. Dá para contornar.", "distanciaMetros": 0 },
    { "texto": "Chegar a Rua Portas do Carmo, à direita", "distanciaMetros": 0 }
  ],
  "obstaculosEvitados": 0,
  "desvioPossivel": true,
  "motor": "salvador"
}
```

- Coordenadas na URL em `lat,lon`; no GeoJSON, `[longitude, latitude]`.
- `perfil` opcional: `wheelchair` (padrão) ou `foot-walking`.
- Erros vêm como `{ "erro": "mensagem" }`: 400 para entrada inválida, 422 sem caminho, 503 se a câmera ou o openrouteservice estiverem fora.

Outros endpoints: `POST /api/rota` (com obstáculos simulados, para testes), `GET /api/obstaculos?bbox=...`, `GET /api/area` e `GET /health`. Detalhes e exemplos no Swagger.

## Formato esperado do back da câmera

A API chama `GET {CAMERA_URL}?bbox=minLon,minLat,maxLon,maxLat` e espera uma lista:

```json
[
  { "latitude": -12.9735, "longitude": -38.5111, "passagem": "bloqueada", "tipo": "degrau", "confianca": 0.87 }
]
```

`latitude` e `longitude` são obrigatórios; `passagem` sem valor conta como `bloqueada`; `tipo` e `confianca` são opcionais. Sem `CAMERA_URL`, a API usa obstáculos de teste fixos.

## Rodar localmente

Precisa de Docker e de uma chave gratuita do openrouteservice ([account.heigit.org](https://account.heigit.org/signup)).

```bash
cd rotas
cp .env.example .env      # preencher ORS_API_KEY
docker compose up -d --build
```

A API sobe em http://localhost:8080. Para conferir tudo de ponta a ponta: `bash testes/testar-api.sh` (as rotas do teste são em Salvador: com `ORS_LOCAL_URL` ligado não gastam cota; sem ele, gastam cerca de 28 chamadas da API pública).

Principais variáveis (lista completa em `rotas/.env.example`):

| Variável | Para quê |
|---|---|
| `ORS_API_KEY` | Chave do openrouteservice |
| `ORS_LOCAL_URL` | openrouteservice próprio (ex.: `http://ors:8082/ors`); vazio usa só a API pública |
| `CAMERA_URL` | Endereço do back da câmera; vazio usa obstáculos de teste |
| `AREA_BBOX` | Área atendida (`minLon,minLat,maxLon,maxLat`); vazio libera o mundo todo |
| `CORS_ORIGENS` | Domínios do front que podem chamar a API |
| `PAGINA_TESTE` | `false` esconde a página de teste |

## Tecnologias

Java 21, Spring Boot 4, springdoc (Swagger), Leaflet na página de teste, openrouteservice, Docker e Caddy na VPS.
