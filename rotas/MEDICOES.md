# Medições do openrouteservice

Medido em 21/10/2026 para saber quanto o ORS custa antes de escrever a API.

## Ambiente
- ORS `v10.1.0` (imagem oficial, também existe em arm64)
- Windows 11 + Docker Desktop (amd64, 4 CPUs, 3,8 GB de RAM para o Docker)
- Dados: Geofabrik `nordeste-latest.osm.pbf` (424 MB), baixado em 21/10/2026
- JVM: `XMS=256m`; `XMX=2g` (Salvador) e `3g` (Maranhão)
- Elevação ligada (SRTM), para o ORS considerar subidas e descidas
- Cada cenário começa do zero: o `medir.sh` apaga os grafos antes

## Resultados

| | 1. Salvador, foot-walking | 2. Salvador, foot-walking + wheelchair | 3. Maranhão, foot-walking + wheelchair |
|---|---|---|---|
| Arquivo `.pbf` | 9,4 MB | 9,4 MB | 36 MB |
| Nós no grafo (por perfil) | 76 mil | 76 mil | 308 mil |
| **Pico de RAM na importação** | **804 MiB** | **1.126 MiB** | **1.983 MiB** |
| **RAM parado, já pronto** | **431 MiB** | **609 MiB** | **1.529 MiB** |
| **Tempo de importação** (até `/health` = ready) | 42 s | 34 s* | 194 s |
| **Disco: grafos** | 24 MB | 49 MB | 184 MB |
| **Disco: relevo (cache SRTM)** | 83 MB | 83 MB | 637 MB |
| Grafo na RAM, segundo o ORS | 23 MB | 47 MB | 176 MB |
| Rota de teste | 200 OK, 542 m, 0,42 s | foot 542 m, wheelchair 571 m | foot 1.126 m, wheelchair 1.153 m |

\* O cenário 2 foi mais rápido que o 1 porque o relevo já estava no cache do Docker Desktop. Sem cache, o esperado é uns 45 a 50 s.

Rotas de teste:
- Salvador: Terreiro de Jesus até a Praça da Sé (`testes/rota-pelourinho.json`)
- São Luís: Praça Dom Pedro II até a Praça Deodoro (`testes/rota-sao-luis.json`)

Nos dois casos a rota `wheelchair` sai um pouco mais longa que a `foot-walking`, sinal de que o perfil está desviando de trechos ruins para cadeira.

## Observações
- **A RAM depende mais da JVM do que do mapa.** O grafo de Salvador ocupa só 47 MB, mas a JVM sozinha fica perto de 400 MiB. Na primeira tentativa, com `XMS=1g`, o cenário 1 ficou em 1.160 MiB parado só por causa da reserva de memória.
- **O pico só acontece na importação.** Depois que o grafo fica pronto ele é reaproveitado, e o ORS sobe sem reconstruir (`ORS_REBUILD=false`).
- **Escala:** o Maranhão tem 4 vezes mais nós que Salvador e precisou de 2,5 vezes a RAM parado e quase 6 vezes o tempo.
- O `docker stats` foi lido a cada ~2 s, então o pico real pode ser um pouco maior que o medido.
- As medições foram feitas em amd64. Em ARM a memória deve ficar parecida (é a mesma JVM), mas a importação pode demorar mais.

## Cabe na VPS (Oracle ARM, 6 a 12 GB, com outros projetos Spring Boot)?

**Salvador com os dois perfis: cabe com folga, inclusive na de 6 GB.**
- ORS: ~0,6 GB parado e ~1,1 GB no pico, que só acontece na importação.
- A nossa API Spring Boot deve somar uns 0,3 a 0,5 GB.
- Total do projeto: ~1 GB no dia a dia e ~1,6 GB durante a importação.

**Maranhão inteiro: cabe na de 12 GB. Na de 6 GB fica apertado**, se os outros projetos já usam 3 GB ou mais (~1,5 GB parado e ~2 GB no pico).

Recomendações para o deploy:
1. Limitar o container: `XMX=1g` e `mem_limit: 1500m` para Salvador. Isso impede que o ORS cresça e derrube os outros projetos.
2. Se a VPS ficar apertada, construir o grafo na máquina local e copiar a pasta `graphs/` para a VPS, que então só carrega o grafo pronto.
3. Reservar ~1 GB de disco para grafos, relevo e `.pbf`. O arquivo do Nordeste (424 MB) pode ser apagado depois do recorte.

## Como repetir
No Git Bash, dentro de `rotas/`:
```bash
./recortar.sh                       # recorta a cidade do .env
# ajustar ORS_PBF, ORS_XMX e ORS_PERFIL_* no .env
./medir.sh <nome-do-cenario> [testes/rota.json]
```
Os resultados ficam em `medicoes/<cenario>/` (`resumo.txt`, `stats.txt`, `ors.log`, rotas em JSON).
