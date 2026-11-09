#!/usr/bin/env bash
set -euo pipefail
PROYECTO_PIZZERIA="$(cd "$(dirname "$0")" && pwd)"
"$PROYECTO_PIZZERIA/verificar.sh"
command -v zip >/dev/null 2>&1 || { echo 'Se requiere zip para preparar la entrega.' >&2; exit 1; }
mkdir -p "$PROYECTO_PIZZERIA/../../entrega"
DESTINO_PIZZERIA="$(cd "$PROYECTO_PIZZERIA/../../entrega" && pwd)/PizzeriaKotlinS3.zip"
TEMPORAL_PIZZERIA="$(mktemp -d "${TMPDIR:-/tmp}/entrega-pizzeria.XXXXXX")"
trap 'rm -rf "$TEMPORAL_PIZZERIA"' EXIT
cd "$PROYECTO_PIZZERIA/.."
zip -q -r "$TEMPORAL_PIZZERIA/PizzeriaKotlinS3.zip" PizzeriaKotlinS3 \
    -x '*/build/*' '*/build/' '*/out/*' '*/out/' '*/bin/*' '*/bin/' '*/.gradle/*' '*/.gradle/' \
       '*/.kotlin/*' '*/.kotlin/' '*/.git/*' '*/.git/' '*/.DS_Store' \
       '*/.idea/workspace.xml' '*/.idea/shelf/*'
mv "$TEMPORAL_PIZZERIA/PizzeriaKotlinS3.zip" "$DESTINO_PIZZERIA"
printf 'Proyecto preparado: %s\n' "$DESTINO_PIZZERIA"
