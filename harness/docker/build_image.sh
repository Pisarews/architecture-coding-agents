#!/usr/bin/env bash
# Builds archagents/sandbox:<tag>. Copies the three FHIR variants and the acceptance suite in as
# warm-up projects so the image's ~/.m2 is complete for offline builds. From tag 2 on the image also
# carries the erosion tools (PMD, SpotBugs, harness/metrics-tool). Never rebuild a tag that running
# experiments use.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../.." && pwd)
TAG=${1:-1}
rm -rf "$HERE/warm" && mkdir -p "$HERE/warm"
for v in none layered hexagonal; do
  rsync -a --exclude target "$ROOT/fhir/variants/$v/" "$HERE/warm/fhir-$v/"
done
rsync -a --exclude target "$ROOT/fhir/acceptance/" "$HERE/warm/fhir-acceptance/"
# Erosion metrics tool (used from tag 2 on; harness/EROSION.md)
rm -rf "$HERE/tools" && mkdir -p "$HERE/tools"
rsync -a --exclude target "$ROOT/harness/metrics-tool/" "$HERE/tools/metrics-tool/"
docker build -t "archagents/sandbox:$TAG" "$HERE"
rm -rf "$HERE/warm" "$HERE/tools"
docker image inspect "archagents/sandbox:$TAG" --format '{{.Id}} {{.Size}}'
