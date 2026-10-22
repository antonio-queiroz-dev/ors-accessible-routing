#!/usr/bin/env bash
# Sobe o ORS do zero (apaga os grafos) e mede RAM, tempo de importação e disco.
# Depois faz uma rota de teste em cada perfil ligado.
# Uso: ./medir.sh <nome-do-cenario> [arquivo-da-rota-de-teste]   (perfis e .pbf vêm do .env)
# Resultado em medicoes/<cenario>/
set -euo pipefail
cd "$(dirname "$0")"
export MSYS_NO_PATHCONV=1
# Junta o arquivo que expõe a porta do ORS em localhost
export COMPOSE_PATH_SEPARATOR=: COMPOSE_FILE=docker-compose.yml:docker-compose.medir.yml

CENARIO="${1:?informe o nome do cenário}"
ROTA_TESTE="${2:-testes/rota-pelourinho.json}"
SAIDA="medicoes/$CENARIO"
rm -rf "$SAIDA"; mkdir -p "$SAIDA"
set -a; . ./.env; set +a
PORTA="${ORS_PORTA:-8082}"

# Apaga só o container e os grafos do ORS. Banco da API (api-dados) e cache de relevo ficam.
docker compose rm -sf ors >/dev/null 2>&1 || true
docker volume rm "$(basename "$PWD")_ors-graphs" >/dev/null 2>&1 || true
inicio=$(date +%s)
docker compose up -d ors

# Leituras do docker stats em segundo plano até existir o arquivo "parar".
# (No Windows, kill no docker stats contínuo não encerra o docker.exe e o script fica preso.)
PARAR="$SAIDA/parar"
( while [ ! -f "$PARAR" ]; do
    docker stats --no-stream --format '{{.MemUsage}}' rotas-ors 2>/dev/null || true
  done > "$SAIDA/stats.txt" ) &
coletor=$!
trap 'touch "$PARAR"' EXIT
until curl -s "http://127.0.0.1:$PORTA/ors/v2/health" | grep -q '"ready"'; do
  if [ "$(docker inspect -f '{{.State.Running}}' rotas-ors)" != "true" ]; then
    echo "ORS parou durante a importação"; docker compose logs --tail 50 ors; exit 1
  fi
  sleep 1
done
pronto=$(( $(date +%s) - inicio ))

# RAM parado: 30 s depois de pronto
sleep 30
touch "$PARAR"; wait $coletor; rm -f "$PARAR"

docker compose logs ors > "$SAIDA/ors.log" 2>&1
docker exec rotas-ors du -sh /home/ors/graphs /home/ors/elevation_cache > "$SAIDA/disco.txt"
for p in $(docker exec rotas-ors ls /home/ors/graphs); do
  docker exec rotas-ors du -sh "/home/ors/graphs/$p" >> "$SAIDA/disco.txt"
done

# Converte "1.234GiB / 3.8GiB" para MiB
para_mib() { awk '{v=$1; u=v; gsub(/[0-9.]/,"",u); gsub(/[A-Za-z]/,"",v);
  if(u=="GiB")v*=1024; else if(u=="KiB")v/=1024; else if(u=="B")v/=1048576; printf "%.0f\n", v}'; }
pico=$(cut -d'/' -f1 "$SAIDA/stats.txt" | para_mib | sort -n | tail -1)
parado=$(tail -10 "$SAIDA/stats.txt" | cut -d'/' -f1 | para_mib | awk '{s+=$1} END {printf "%.0f", s/NR}')

{
  echo "cenario=$CENARIO"
  echo "pbf=${ORS_PBF} foot-walking=${ORS_PERFIL_FOOT_WALKING} wheelchair=${ORS_PERFIL_WHEELCHAIR} xms=${ORS_XMS} xmx=${ORS_XMX}"
  echo "tempo_ate_pronto_s=$pronto"
  echo "ram_pico_importacao_mib=$pico"
  echo "ram_parado_mib=$parado"
  echo "leituras_docker_stats=$(wc -l < "$SAIDA/stats.txt")"
  echo "disco:"; sed 's/^/  /' "$SAIDA/disco.txt"
  grep -oE 'Memory usage by profiles|\[[0-9]\] [0-9.]+ MB' "$SAIDA/ors.log" | sed 's/^/  grafo na RAM: /' | grep MB || true
  echo "rotas de teste ($ROTA_TESTE):"
  for perfil in foot-walking wheelchair; do
    var="ORS_PERFIL_$(echo "$perfil" | tr 'a-z-' 'A-Z_')"
    [ "${!var}" = "true" ] || continue
    curl -s -X POST "http://127.0.0.1:$PORTA/ors/v2/directions/$perfil/geojson" \
      -H 'Content-Type: application/json' -d @"$ROTA_TESTE" -o "$SAIDA/rota-$perfil.json" \
      -w "  $perfil: HTTP %{http_code} em %{time_total}s, " || true
    grep -oE '"summary":\{[^}]*\}' "$SAIDA/rota-$perfil.json" || head -c 300 "$SAIDA/rota-$perfil.json"
  done
} | tee "$SAIDA/resumo.txt"
