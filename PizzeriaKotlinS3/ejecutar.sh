#!/usr/bin/env bash
set -euo pipefail
PROYECTO_PIZZERIA="$(cd "$(dirname "$0")" && pwd)"
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)"
fi
if [[ "${1:-}" == "--demo" && "$#" -eq 1 ]]; then
    exec "$PROYECTO_PIZZERIA/gradlew" -p "$PROYECTO_PIZZERIA" --quiet --console=plain demostracion
fi
exec "$PROYECTO_PIZZERIA/gradlew" -p "$PROYECTO_PIZZERIA" --quiet --console=plain run "$@"
