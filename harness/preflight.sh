#!/usr/bin/env bash
# Checks the OpenRouter key and model availability without spending anything. Never prints the key.
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
if [[ -z "${OPENROUTER_API_KEY:-}" && -f "$HOME/.config/archagents/env" ]]; then
  set -a; source "$HOME/.config/archagents/env"; set +a
fi
: "${OPENROUTER_API_KEY:?no key: set OPENROUTER_API_KEY or create ~/.config/archagents/env}"
echo "== key"
curl -s https://openrouter.ai/api/v1/key -H "Authorization: Bearer $OPENROUTER_API_KEY" | "$HERE/../../.venv/bin/python" -c '
import json,sys
d=json.load(sys.stdin).get("data",{})
print("  label:", d.get("label"), "| usage $:", d.get("usage"), "| limit $:", d.get("limit"), "| free tier only:", d.get("is_free_tier"))
print("  -> 1000 free req/day unlocked" if d.get("is_free_tier") is False else "  -> NOTE: still on 50 free req/day; top up >= $10 first")'
echo "== models"
"$HERE/../../.venv/bin/python" "$HERE/check_models.py"
echo "== docker image"
docker image inspect archagents/sandbox:1 --format '  archagents/sandbox:1 present ({{.Size}} bytes)' 2>/dev/null || echo "  MISSING: run harness/docker/build_image.sh"
