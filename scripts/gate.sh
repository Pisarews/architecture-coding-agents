#!/usr/bin/env bash
# Equivalence gate for one variant: static checks, build (incl. its own unit + ArchUnit tests),
# then the domain's black-box acceptance suite against the running jar.
#
#   scripts/gate.sh fhir none
#   scripts/gate.sh fhir layered
#   scripts/gate.sh fhir hexagonal
set -euo pipefail

DOMAIN=${1:?domain (fhir|orders)}
VARIANT=${2:?variant (none|layered|hexagonal)}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP="$ROOT/$DOMAIN/variants/$VARIANT"
ACC="$ROOT/$DOMAIN/acceptance"
LOG_DIR="$ROOT/$DOMAIN/gate-logs"
mkdir -p "$LOG_DIR"

if [[ -z "${JAVA_HOME:-}" ]]; then
  for c in /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home; do
    [[ -x "$c/bin/java" ]] && export JAVA_HOME="$c" && break
  done
fi
JAVA="$JAVA_HOME/bin/java"

fail() { echo "GATE FAIL [$DOMAIN/$VARIANT]: $*" >&2; exit 1; }

echo "== static checks"
cmp -s "$ROOT/$DOMAIN/schema/V1__baseline.sql" "$APP/src/main/resources/db/migration/V1__baseline.sql" \
  || fail "Flyway schema differs from $DOMAIN/schema/V1__baseline.sql"
python3 "$ROOT/scripts/check_pom.py" "$DOMAIN" "$APP/pom.xml" || fail "dependency set differs"

echo "== build + own tests"
(cd "$APP" && mvn -q -B clean package) > "$LOG_DIR/$VARIANT-build.log" 2>&1 \
  || { tail -40 "$LOG_DIR/$VARIANT-build.log"; fail "build/tests failed (see gate-logs/$VARIANT-build.log)"; }
JAR=$(ls "$APP"/target/*.jar | grep -v original | head -1)

PORT=$(python3 -c 'import socket; s=socket.socket(); s.bind(("",0)); print(s.getsockname()[1])')
echo "== start $JAR on :$PORT"
"$JAVA" -jar "$JAR" --server.port="$PORT" > "$LOG_DIR/$VARIANT-server.log" 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true' EXIT
for _ in $(seq 1 90); do
  curl -sf "http://localhost:$PORT/fhir/metadata" >/dev/null 2>&1 && break
  curl -sf "http://localhost:$PORT/actuator/health" >/dev/null 2>&1 && break
  kill -0 $PID 2>/dev/null || { tail -40 "$LOG_DIR/$VARIANT-server.log"; fail "server died"; }
  sleep 1
done

echo "== acceptance suite"
# private copy so parallel gate runs do not share target/
ACC_RUN=$(mktemp -d)
cp -R "$ACC/pom.xml" "$ACC/src" "$ACC_RUN/"
trap 'kill $PID 2>/dev/null || true; rm -rf "$ACC_RUN"' EXIT
if (cd "$ACC_RUN" && mvn -q -B test -Dbase.url="http://localhost:$PORT") > "$LOG_DIR/$VARIANT-acceptance.log" 2>&1; then
  echo "GATE PASS [$DOMAIN/$VARIANT]"
else
  grep -E "Tests run:|FAIL|expected" "$LOG_DIR/$VARIANT-acceptance.log" | head -60
  fail "acceptance suite failed (see gate-logs/$VARIANT-acceptance.log)"
fi
